package com.dyh.salesAgent.controller;
import com.dyh.salesAgent.agent.SalesAgent;
import com.dyh.salesAgent.memory.MysqlChatMemoryStore;
import com.dyh.salesAgent.memory.UserScopedMemoryId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
public class SalesAgentController {

    private final SalesAgent salesAgent;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("接收请求: sessionId={}, message={}", request.sessionId(), request.message());
        long start = System.currentTimeMillis();

        String memoryId = UserScopedMemoryId.fromCurrentUser(request.sessionId());
        String reply = salesAgent.chat(memoryId, request.message(), LocalDate.now().toString());

        long duration = System.currentTimeMillis() - start;
        log.info("请求完成: sessionId={}, durationMs={}", request.sessionId(), duration);

        return ResponseEntity.ok(new ChatResponse(request.sessionId(), reply, duration));
    }

    private final MysqlChatMemoryStore chatMemoryStore;

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatMemoryStore.deleteMessages(UserScopedMemoryId.fromCurrentUser(sessionId));
        return ResponseEntity.ok().build();
    }
}
