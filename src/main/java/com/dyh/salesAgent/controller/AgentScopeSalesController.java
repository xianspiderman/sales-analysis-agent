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

/** AgentScope 单 ReActAgent 与 Supervisor 多 Agent 入口。 */
@RestController
@RequestMapping("/agentscope")
@RequiredArgsConstructor
@Slf4j
public class AgentScopeSalesController {

    private final AgentScopeSalesService agentScopeSalesService;

    @PostMapping("/chat")
    public Mono<ResponseEntity<AgentScopeChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        return agentScopeSalesService.chat(request.sessionId(), request.message())
                .map(result -> ResponseEntity.ok(new AgentScopeChatResponse(
                        request.sessionId(), result.reply(), result.execution())));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> chatStream(@Valid @RequestBody ChatRequest request) {
        return agentScopeSalesService.stream(request.sessionId(), request.message())
                .map(event -> ServerSentEvent.<Object>builder()
                        .event(event.event())
                        .data(event.data())
                        .build());
    }

    @DeleteMapping("/session/{sessionId}")
    public Mono<ResponseEntity<Void>> clearSession(@PathVariable String sessionId) {
        return agentScopeSalesService.clearSession(sessionId)
                .thenReturn(ResponseEntity.ok().build());
    }

    @PostMapping("/team/chat")
    public Mono<ResponseEntity<AgentScopeChatResponse>> teamChat(
            @Valid @RequestBody ChatRequest request) {
        return agentScopeSalesService.teamChat(request.sessionId(), request.message())
                .map(result -> ResponseEntity.ok(new AgentScopeChatResponse(
                        request.sessionId(), result.reply(), result.execution())));
    }

    @PostMapping(value = "/team/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> teamChatStream(
            @Valid @RequestBody ChatRequest request) {
        return agentScopeSalesService.teamStream(request.sessionId(), request.message())
                .map(event -> ServerSentEvent.<Object>builder()
                        .event(event.event())
                        .data(event.data())
                        .build());
    }

    @DeleteMapping("/team/session/{sessionId}")
    public Mono<ResponseEntity<Void>> clearTeamSession(@PathVariable String sessionId) {
        return agentScopeSalesService.clearTeamSession(sessionId)
                .thenReturn(ResponseEntity.ok().build());
    }
}
