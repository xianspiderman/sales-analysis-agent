package com.dyh.salesAgent.memory;

import com.dyh.salesAgent.security.UserContext;

/**
 * 把客户端会话 ID 转换为按登录用户隔离的内部记忆 ID。
 */
public final class UserScopedMemoryId {

    public static final int MAX_CLIENT_SESSION_ID_LENGTH = 80;

    private UserScopedMemoryId() {
    }

    public static String fromCurrentUser(String clientSessionId) {
        return from(UserContext.requireCurrent(), clientSessionId);
    }

    public static String from(UserContext.UserInfo user, String clientSessionId) {
        if (user == null || user.userId() == null) {
            throw new IllegalStateException("用户上下文缺失，无法访问对话记忆");
        }
        if (clientSessionId == null || clientSessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (clientSessionId.length() > MAX_CLIENT_SESSION_ID_LENGTH) {
            throw new IllegalArgumentException("sessionId 不能超过 80 个字符");
        }
        return user.userId() + ":" + clientSessionId;
    }
}
