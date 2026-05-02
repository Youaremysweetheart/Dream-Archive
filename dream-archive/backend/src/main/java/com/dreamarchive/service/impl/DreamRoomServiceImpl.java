package com.dreamarchive.service.impl;

import com.dreamarchive.common.DreamRoomConstants;
import com.dreamarchive.common.PageResult;
import com.dreamarchive.dto.dify.DifyWorkflowInput;
import com.dreamarchive.dto.dify.DifyWorkflowOutput;
import com.dreamarchive.dto.dreamroom.DreamRoomEnterResponse;
import com.dreamarchive.dto.dreamroom.DreamRoomHistoryItem;
import com.dreamarchive.dto.dreamroom.DreamRoomSendRequest;
import com.dreamarchive.dto.dreamroom.DreamRoomSendResponse;
import com.dreamarchive.entity.Dream;
import com.dreamarchive.entity.DreamRoom;
import com.dreamarchive.entity.DreamRoomAiTask;
import com.dreamarchive.entity.DreamRoomMessage;
import com.dreamarchive.mapper.DreamMapper;
import com.dreamarchive.mapper.DreamRoomAiTaskMapper;
import com.dreamarchive.mapper.DreamRoomMapper;
import com.dreamarchive.mapper.DreamRoomMessageMapper;
import com.dreamarchive.service.DifyWorkflowClient;
import com.dreamarchive.service.DreamRoomService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class DreamRoomServiceImpl implements DreamRoomService {

    private static final Logger log = LoggerFactory.getLogger(DreamRoomServiceImpl.class);

    private final DreamMapper dreamMapper;
    private final DreamRoomMapper dreamRoomMapper;
    private final DreamRoomMessageMapper messageMapper;
    private final DreamRoomAiTaskMapper taskMapper;
    private final DifyWorkflowClient difyWorkflowClient;

    private final int queuePollMs;
    private final int queueBatchSize;
    private final int queueMaxRetry;

    public DreamRoomServiceImpl(DreamMapper dreamMapper,
                                DreamRoomMapper dreamRoomMapper,
                                DreamRoomMessageMapper messageMapper,
                                DreamRoomAiTaskMapper taskMapper,
                                DifyWorkflowClient difyWorkflowClient,
                                @Value("${dream-room.queue.poll-ms:1500}") int queuePollMs,
                                @Value("${dream-room.queue.batch-size:10}") int queueBatchSize,
                                @Value("${dream-room.queue.max-retry:3}") int queueMaxRetry) {
        this.dreamMapper = dreamMapper;
        this.dreamRoomMapper = dreamRoomMapper;
        this.messageMapper = messageMapper;
        this.taskMapper = taskMapper;
        this.difyWorkflowClient = difyWorkflowClient;
        this.queuePollMs = Math.max(500, queuePollMs);
        this.queueBatchSize = Math.max(1, queueBatchSize);
        this.queueMaxRetry = Math.max(1, queueMaxRetry);
    }

    @PostConstruct
    public void logQueueStartup() {
        log.info("Dream room queue worker started. pollMs={}, batchSize={}, maxRetry={}",
                queuePollMs, queueBatchSize, queueMaxRetry);
    }

    @Override
    @Transactional
    public DreamRoom initRoomAfterAnalyze(Dream dream) {
        if (dream == null || dream.getId() == null || dream.getUserId() == null) {
            return null;
        }
        DreamRoom room = dreamRoomMapper.findByUserAndDreamPost(dream.getUserId(), dream.getId());
        if (room == null) {
            room = createRoom(dream.getUserId(), dream.getId(), DreamRoomConstants.ROOM_STATUS_ABNORMAL);
        } else if (room.getDreamRoomStatus() != DreamRoomConstants.ROOM_STATUS_BANNED) {
            dreamRoomMapper.updateStatusAndOpening(
                    room.getDreamRoomId(),
                    DreamRoomConstants.ROOM_STATUS_ABNORMAL,
                    0
            );
            room = dreamRoomMapper.findByDreamRoomId(room.getDreamRoomId());
        }
        return room;
    }

    @Override
    @Transactional
    public DreamRoomEnterResponse enterRoom(Long currentUserId, Long dreamPostId) {
        if (currentUserId == null) {
            return buildEnterResponse(null, null, DreamRoomConstants.ROOM_STATUS_ABNORMAL,
                    "Please publish today's dream post first.", false);
        }

        Dream dream = resolveTodayDream(currentUserId, dreamPostId);
        if (dream == null) {
            return buildEnterResponse(null, null, DreamRoomConstants.ROOM_STATUS_ABNORMAL,
                    "No dream post found for today. Please create one first.", false);
        }

        DreamRoom room = ensureRoomExists(currentUserId, dream.getId(), DreamRoomConstants.ROOM_STATUS_ABNORMAL);
        return initializeAndEnterRoom(room, dream);
    }

    @Override
    @Transactional
    public DreamRoomEnterResponse enterRoomByPost(Long currentUserId, Long dreamPostId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        if (dreamPostId == null) {
            throw new IllegalArgumentException("dream_post_id is required");
        }

        Dream dream = dreamMapper.findById(dreamPostId);
        if (dream == null) {
            throw new IllegalArgumentException("Dream post does not exist");
        }
        if (!currentUserId.equals(dream.getUserId())) {
            throw new IllegalArgumentException("No permission to access this dream post");
        }

        DreamRoom room = ensureRoomExists(currentUserId, dreamPostId, DreamRoomConstants.ROOM_STATUS_ABNORMAL);
        return initializeAndEnterRoom(room, dream);
    }

    @Override
    public List<DreamRoomHistoryItem> getRoomHistory(Long currentUserId, int limit) {
        if (currentUserId == null) {
            return Collections.emptyList();
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<Dream> dreams = dreamMapper.findByUserId(currentUserId, 0, safeLimit);
        if (dreams == null || dreams.isEmpty()) {
            return Collections.emptyList();
        }

        List<DreamRoomHistoryItem> result = new ArrayList<>(dreams.size());
        for (Dream dream : dreams) {
            DreamRoom room = dreamRoomMapper.findByUserAndDreamPost(currentUserId, dream.getId());
            DreamRoomHistoryItem item = new DreamRoomHistoryItem();
            item.setDreamPostId(dream.getId());
            item.setDreamTitle(dream.getTitle());
            item.setDreamDate(dream.getDreamDate());
            item.setDreamCreateTime(dream.getCreateTime());
            item.setDreamRoomId(room == null ? null : room.getDreamRoomId());
            item.setDreamRoomStatus(room == null ? DreamRoomConstants.ROOM_STATUS_ABNORMAL : room.getDreamRoomStatus());
            item.setRoomExists(room != null);
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public DreamRoomSendResponse sendMessage(Long currentUserId, DreamRoomSendRequest request) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        if (request == null || request.getDreamRoomId() == null || request.getDreamRoomId().isBlank()) {
            throw new IllegalArgumentException("dream_room_id is required");
        }
        String text = request.getText() == null ? "" : request.getText().trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }

        DreamRoom room = dreamRoomMapper.findByDreamRoomId(request.getDreamRoomId().trim());
        if (room == null || !currentUserId.equals(room.getUserId())) {
            throw new IllegalArgumentException("Room does not exist or no permission");
        }
        if (request.getDreamPostId() != null && !request.getDreamPostId().equals(room.getDreamPostId())) {
            throw new IllegalArgumentException("dream_post_id does not match room");
        }
        if (room.getDreamRoomStatus() == DreamRoomConstants.ROOM_STATUS_BANNED) {
            throw new IllegalStateException("Violation detected. Please leave.");
        }
        if (room.getDreamRoomStatus() != DreamRoomConstants.ROOM_STATUS_CHAT) {
            throw new IllegalStateException("Opening message is still being generated.");
        }

        Dream dream = dreamMapper.findById(room.getDreamPostId());
        if (dream == null) {
            throw new IllegalArgumentException("Dream post does not exist");
        }

        DreamRoomMessage userMessage = new DreamRoomMessage();
        userMessage.setDreamRoomId(room.getDreamRoomId());
        userMessage.setUserId(currentUserId);
        userMessage.setDreamPostId(room.getDreamPostId());
        userMessage.setSenderId(currentUserId);
        userMessage.setMessageRole(DreamRoomConstants.MESSAGE_ROLE_QUESTION);
        userMessage.setMessageText(text);
        userMessage.setIsViolation(0);
        userMessage.setClientMsgId(normalizeClientMsgId(request.getClientMsgId()));

        try {
            messageMapper.insert(userMessage);
        } catch (DuplicateKeyException ex) {
            log.info("Duplicate message ignored: room={}, clientMsgId={}", room.getDreamRoomId(), request.getClientMsgId());
            return new DreamRoomSendResponse(true, room.getDreamRoomId(), room.getDreamRoomStatus());
        }

        enqueueTask(room, dream.getContent(), text, DreamRoomConstants.TASK_TYPE_QA);
        return new DreamRoomSendResponse(true, room.getDreamRoomId(), room.getDreamRoomStatus());
    }

    @Override
    public PageResult<DreamRoomMessage> getMessages(Long currentUserId, String dreamRoomId, int pageNum, int pageSize) {
        if (currentUserId == null || dreamRoomId == null || dreamRoomId.isBlank()) {
            return new PageResult<>(0L, pageNum, pageSize, Collections.emptyList());
        }
        DreamRoom room = dreamRoomMapper.findByDreamRoomId(dreamRoomId.trim());
        if (room == null || !currentUserId.equals(room.getUserId())) {
            return new PageResult<>(0L, pageNum, pageSize, Collections.emptyList());
        }
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int offset = (safePageNum - 1) * safePageSize;

        List<DreamRoomMessage> records = messageMapper.findByRoomPage(room.getDreamRoomId(), offset, safePageSize);
        long total = messageMapper.countByRoom(room.getDreamRoomId());
        return new PageResult<>(total, safePageNum, safePageSize, records);
    }

    @Scheduled(fixedDelayString = "${dream-room.queue.poll-ms:1500}")
    public void processQueue() {
        List<DreamRoomAiTask> tasks = taskMapper.findPending(queueBatchSize);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        log.info("Dream room queue picked {} task(s).", tasks.size());
        for (DreamRoomAiTask task : tasks) {
            if (task == null || task.getId() == null) continue;
            if (taskMapper.markProcessing(task.getId()) == 0) continue;
            processTask(task);
        }
    }

    private void processTask(DreamRoomAiTask task) {
        try {
            DifyWorkflowInput input = new DifyWorkflowInput();
            input.setUserId(task.getUserId());
            input.setDreamPostId(task.getDreamPostId());
            input.setDreamRoomId(task.getDreamRoomId());
            input.setDreamRoomStatus(task.getDreamRoomStatus());
            input.setDreamPostContent(task.getDreamPostContent());
            input.setQuestion(task.getQuestion());

            DifyWorkflowOutput output = difyWorkflowClient.run(input);
            String answer = output.getAnswer();
            boolean violation = output.isViolation();

            taskMapper.markSuccess(task.getId(), answer, violation ? 1 : 0);
            handleAiOutput(task, answer, violation);
        } catch (Exception ex) {
            int retryCount = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
            int nextStatus = retryCount >= queueMaxRetry
                    ? DreamRoomConstants.TASK_STATUS_FAILED
                    : DreamRoomConstants.TASK_STATUS_PENDING;
            taskMapper.updateRetry(task.getId(), nextStatus, retryCount, trimError(ex.getMessage()));
            log.warn("Dream room task failed: taskId={}, retry={}, error={}", task.getTaskId(), retryCount, ex.getMessage());
        }
    }

    @Transactional
    protected void handleAiOutput(DreamRoomAiTask task, String answer, boolean violation) {
        DreamRoom room = dreamRoomMapper.findByDreamRoomId(task.getDreamRoomId());
        if (room == null) return;

        if (violation) {
            String violationText = (answer == null || answer.isBlank())
                    ? "Violation detected. Counseling service has been stopped."
                    : answer;
            dreamRoomMapper.banRoom(room.getDreamRoomId(), "dify_violation");
            insertAssistantMessage(room, violationText, DreamRoomConstants.MESSAGE_ROLE_VIOLATION, 1);
            return;
        }

        if (task.getTaskType() != null && task.getTaskType() == DreamRoomConstants.TASK_TYPE_OPENING) {
            insertAssistantMessage(room, answer, DreamRoomConstants.MESSAGE_ROLE_OPENING, 0);
            dreamRoomMapper.updateStatusAndOpening(
                    room.getDreamRoomId(),
                    DreamRoomConstants.ROOM_STATUS_CHAT,
                    1
            );
            return;
        }

        insertAssistantMessage(room, answer, DreamRoomConstants.MESSAGE_ROLE_ANSWER, 0);
    }

    private DreamRoomEnterResponse initializeAndEnterRoom(DreamRoom room, Dream dream) {
        if (room.getDreamRoomStatus() == DreamRoomConstants.ROOM_STATUS_BANNED) {
            return buildEnterResponse(dream.getId(), room.getDreamRoomId(), DreamRoomConstants.ROOM_STATUS_BANNED,
                    "Violation detected. Please leave.", false);
        }

        if (room.getDreamRoomStatus() == DreamRoomConstants.ROOM_STATUS_ABNORMAL) {
            int updated = dreamRoomMapper.updateStatusIfMatch(
                    room.getDreamRoomId(),
                    DreamRoomConstants.ROOM_STATUS_ABNORMAL,
                    DreamRoomConstants.ROOM_STATUS_FIRST_ENTER
            );
            if (updated > 0) {
                room.setDreamRoomStatus(DreamRoomConstants.ROOM_STATUS_FIRST_ENTER);
                room.setOpeningMessageGenerated(0);
            } else {
                room = dreamRoomMapper.findByDreamRoomId(room.getDreamRoomId());
                if (room == null) {
                    throw new IllegalStateException("Room state changed unexpectedly");
                }
            }
        }

        if (room.getDreamRoomStatus() == DreamRoomConstants.ROOM_STATUS_FIRST_ENTER) {
            if ((room.getOpeningMessageGenerated() == null || room.getOpeningMessageGenerated() == 0)
                    && taskMapper.countActiveByRoomAndType(room.getDreamRoomId(), DreamRoomConstants.TASK_TYPE_OPENING) == 0) {
                enqueueTask(room, dream.getContent(), "", DreamRoomConstants.TASK_TYPE_OPENING);
            }
            return buildEnterResponse(dream.getId(), room.getDreamRoomId(), DreamRoomConstants.ROOM_STATUS_FIRST_ENTER,
                    "Assistant is preparing the opening message.", false);
        }

        return buildEnterResponse(dream.getId(), room.getDreamRoomId(), DreamRoomConstants.ROOM_STATUS_CHAT,
                "Room is ready.", true);
    }

    private Dream resolveTodayDream(Long currentUserId, Long dreamPostId) {
        if (dreamPostId == null) {
            return dreamMapper.findTodayLatestByUserId(currentUserId);
        }
        Dream inputDream = dreamMapper.findById(dreamPostId);
        if (inputDream != null
                && currentUserId.equals(inputDream.getUserId())
                && isTodayDream(inputDream)) {
            return inputDream;
        }
        return dreamMapper.findTodayLatestByUserId(currentUserId);
    }

    private boolean isTodayDream(Dream dream) {
        if (dream == null || dream.getCreateTime() == null) return false;
        return LocalDate.now().equals(dream.getCreateTime().toLocalDate());
    }

    private DreamRoom ensureRoomExists(Long userId, Long dreamPostId, int initialStatus) {
        DreamRoom room = dreamRoomMapper.findByUserAndDreamPost(userId, dreamPostId);
        if (room != null) return room;
        try {
            return createRoom(userId, dreamPostId, initialStatus);
        } catch (DuplicateKeyException ex) {
            DreamRoom existed = dreamRoomMapper.findByUserAndDreamPost(userId, dreamPostId);
            if (existed != null) return existed;
            throw ex;
        }
    }

    private DreamRoom createRoom(Long userId, Long dreamPostId, int initialStatus) {
        DreamRoom room = new DreamRoom();
        room.setDreamRoomId(buildDreamRoomId(userId, dreamPostId));
        room.setUserId(userId);
        room.setDreamPostId(dreamPostId);
        room.setDreamRoomStatus(initialStatus);
        room.setOpeningMessageGenerated(0);
        room.setDifyConversationId(null);
        room.setBannedReason(null);
        dreamRoomMapper.insert(room);
        return room;
    }

    private void enqueueTask(DreamRoom room, String dreamPostContent, String question, int taskType) {
        DreamRoomAiTask task = new DreamRoomAiTask();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setDreamRoomId(room.getDreamRoomId());
        task.setUserId(room.getUserId());
        task.setDreamPostId(room.getDreamPostId());
        task.setDreamPostContent(dreamPostContent == null ? "" : dreamPostContent);
        task.setDreamRoomStatus(room.getDreamRoomStatus());
        task.setQuestion(question == null ? "" : question);
        task.setAnswer(null);
        task.setIsViolation(0);
        task.setTaskType(taskType);
        task.setTaskStatus(DreamRoomConstants.TASK_STATUS_PENDING);
        task.setRetryCount(0);
        task.setLastError(null);
        taskMapper.insert(task);
    }

    private void insertAssistantMessage(DreamRoom room, String text, int messageRole, int isViolation) {
        DreamRoomMessage assistantMessage = new DreamRoomMessage();
        assistantMessage.setDreamRoomId(room.getDreamRoomId());
        assistantMessage.setUserId(room.getUserId());
        assistantMessage.setDreamPostId(room.getDreamPostId());
        assistantMessage.setSenderId(DreamRoomConstants.ASSISTANT_SENDER_ID);
        assistantMessage.setMessageRole(messageRole);
        assistantMessage.setMessageText(text == null ? "" : text);
        assistantMessage.setIsViolation(isViolation);
        assistantMessage.setClientMsgId(null);
        messageMapper.insert(assistantMessage);
    }

    private DreamRoomEnterResponse buildEnterResponse(Long dreamPostId,
                                                      String dreamRoomId,
                                                      int status,
                                                      String tip,
                                                      boolean canChat) {
        return new DreamRoomEnterResponse(dreamPostId, dreamRoomId == null ? "" : dreamRoomId, status, tip, canChat);
    }

    private String buildDreamRoomId(Long userId, Long dreamPostId) {
        return "dr_" + userId + "_" + dreamPostId;
    }

    private String normalizeClientMsgId(String clientMsgId) {
        if (clientMsgId == null) return null;
        String normalized = clientMsgId.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String trimError(String error) {
        if (error == null) return "";
        String text = error.trim();
        if (text.length() <= 480) return text;
        return text.substring(0, 480);
    }
}
