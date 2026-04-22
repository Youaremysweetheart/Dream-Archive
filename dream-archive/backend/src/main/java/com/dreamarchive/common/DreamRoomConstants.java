package com.dreamarchive.common;

public final class DreamRoomConstants {

    private DreamRoomConstants() {
    }

    public static final int ROOM_STATUS_ABNORMAL = 0;
    public static final int ROOM_STATUS_FIRST_ENTER = 1;
    public static final int ROOM_STATUS_CHAT = 2;
    public static final int ROOM_STATUS_BANNED = 3;

    public static final int MESSAGE_ROLE_OPENING = 1;
    public static final int MESSAGE_ROLE_QUESTION = 2;
    public static final int MESSAGE_ROLE_ANSWER = 3;
    public static final int MESSAGE_ROLE_VIOLATION = 4;
    public static final int MESSAGE_ROLE_SYSTEM = 5;

    public static final int TASK_TYPE_OPENING = 1;
    public static final int TASK_TYPE_QA = 2;

    public static final int TASK_STATUS_PENDING = 0;
    public static final int TASK_STATUS_PROCESSING = 1;
    public static final int TASK_STATUS_SUCCESS = 2;
    public static final int TASK_STATUS_FAILED = 3;

    public static final long ASSISTANT_SENDER_ID = 0L;
}
