package com.dyh.salesAgent.controller;

import com.dyh.salesAgent.agent.agentscope.AgentScopeSalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 与旧 /agent 接口并行存在的 AgentScope Java 实验入口。 */
@RestController
@RequestMapping("/agentscope")
@RequiredArgsConstructor
@Slf4j
public class AgentScopeSalesController {

    private final AgentScopeSalesService agentScopeSalesService;

    @PostMapping("/chat")
    public Mono<ResponseEntity<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        long start = System.currentTimeMillis();
        return agentScopeSalesService.chat(request.sessionId(), request.message())
                .map(reply -> ResponseEntity.ok(new ChatResponse(
                        request.sessionId(), reply, System.currentTimeMillis() - start)));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@Valid @RequestBody ChatRequest request) {
        return agentScopeSalesService.stream(request.sessionId(), request.message())
                .map(event -> ServerSentEvent.<String>builder()
                        .event(event.event())
                        .data(event.data())
                        .build());
    }

    @DeleteMapping("/session/{sessionId}")
    public Mono<ResponseEntity<Void>> clearSession(@PathVariable String sessionId) {
        return agentScopeSalesService.clearSession(sessionId)
                .thenReturn(ResponseEntity.ok().build());
    }
}
