package com.dyh.salesAgent.controller;

import com.dyh.salesAgent.agent.routing.AgentRouteMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.dyh.salesAgent.memory.UserScopedMemoryId.MAX_CLIENT_SESSION_ID_LENGTH;

/** 统一入口请求；mode 为空时使用应用配置的默认模式。 */
public record RoutedChatRequest(
        @NotBlank(message = "sessionId 不能为空")
        @Size(max = MAX_CLIENT_SESSION_ID_LENGTH, message = "sessionId 不能超过 80 个字符")
        String sessionId,

        @NotBlank(message = "message 不能为空")
        @Size(max = 2000, message = "消息不能超过 2000 字")
        String message,

        AgentRouteMode mode
) {
}
