package com.dreamarchive.common;

/**
 * 梦境疏导房间相关常量：房间状态、消息角色、AI 任务类型与状态。
 */
public final class DreamRoomConstants {

    private DreamRoomConstants() {
    }

    /** 房间状态：异常 / 首次进入 / 可对话 / 已封禁 */
    public static final int ROOM_STATUS_ABNORMAL = 0;
    public static final int ROOM_STATUS_FIRST_ENTER = 1;
    public static final int ROOM_STATUS_CHAT = 2;
    public static final int ROOM_STATUS_BANNED = 3;

    /** 消息角色：开场白、用户提问、助手回答、违规提示、系统 */
    public static final int MESSAGE_ROLE_OPENING = 1;
    public static final int MESSAGE_ROLE_QUESTION = 2;
    public static final int MESSAGE_ROLE_ANSWER = 3;
    public static final int MESSAGE_ROLE_VIOLATION = 4;
    public static final int MESSAGE_ROLE_SYSTEM = 5;

    /** 任务类型：生成开场白、问答 */
    public static final int TASK_TYPE_OPENING = 1;
    public static final int TASK_TYPE_QA = 2;

    /** 任务状态：待处理、处理中、成功、失败 */
    public static final int TASK_STATUS_PENDING = 0;
    public static final int TASK_STATUS_PROCESSING = 1;
    public static final int TASK_STATUS_SUCCESS = 2;
    public static final int TASK_STATUS_FAILED = 3;

    /** 助手消息的占位发送者 ID（非真实用户） */
    public static final long ASSISTANT_SENDER_ID = 0L;
}
