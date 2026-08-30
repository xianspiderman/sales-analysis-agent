package com.dyh.salesAgent.controller;

import com.dyh.salesAgent.agent.routing.AgentRouteMode;
import com.dyh.salesAgent.agent.routing.SalesAgentRoutingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 三种 Agent 模式的统一、可配置入口。 */
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class UnifiedSalesAgentController {

    private final SalesAgentRoutingService routingService;

    @PostMapping("/chat")
    public Mono<ResponseEntity<RoutedChatResponse>> chat(
            @Valid @RequestBody RoutedChatRequest request) {
        return routingService.chat(request.sessionId(), request.message(), request.mode())
                .map(result -> ResponseEntity.ok(new RoutedChatResponse(
                        request.sessionId(),
                        result.reply(),
                        result.route(),
                        result.execution())));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> chatStream(
            @Valid @RequestBody RoutedChatRequest request) {
        return routingService.stream(request.sessionId(), request.message(), request.mode())
                .map(event -> ServerSentEvent.<Object>builder()
                        .event(event.event())
                        .data(event.data())
                        .build());
    }

    @DeleteMapping("/session/{sessionId}")
    public Mono<ResponseEntity<Void>> clearSession(
            @PathVariable String sessionId,
            @RequestParam(required = false) AgentRouteMode mode) {
        return routingService.clearSession(sessionId, mode)
                .thenReturn(ResponseEntity.ok().build());
    }
}
