package com.dyh.salesAgent.controller;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.dyh.salesAgent.memory.UserScopedMemoryId.MAX_CLIENT_SESSION_ID_LENGTH;

public record ChatRequest(
        @NotBlank(message = "sessionId 不能为空")
        @Size(max = MAX_CLIENT_SESSION_ID_LENGTH, message = "sessionId 不能超过 80 个字符")
        String sessionId,

        @NotBlank(message = "message 不能为空")
        @Size(max = 2000, message = "消息不能超过 2000 字")
        String message
) {}
