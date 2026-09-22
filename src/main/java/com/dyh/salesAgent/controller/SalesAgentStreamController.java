package com.dyh.salesAgent.controller;
import com.dyh.salesAgent.agent.SalesAgent;
import com.dyh.salesAgent.memory.UserScopedMemoryId;
import com.dyh.salesAgent.security.UserContext;
import dev.langchain4j.service.TokenStream;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.LocalDate;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
public class SalesAgentStreamController {

    private final SalesAgent salesAgent;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@Valid @RequestBody ChatRequest request) {

        log.info("流式请求: sessionId={}", request.sessionId());

        UserContext.UserInfo userSnapshot = UserContext.requireCurrent();
        String memoryId = UserScopedMemoryId.from(userSnapshot, request.sessionId());

        return Flux.create(sink -> {
            UserContext.runWith(userSnapshot, () -> salesAgent
                    .chatStream(memoryId, request.message(), LocalDate.now().toString())
                    .onPartialResponse(token -> {
                        sink.next(ServerSentEvent.<String>builder()
                                .event("token")
                                .data(token)
                                .build());
                    })
                    .onCompleteResponse(response -> {
                        sink.next(ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build());
                        sink.complete();
                        log.info("流式响应完成: sessionId={}", request.sessionId());
                    })
                    .onError(error -> {
                        log.error("流式响应出错: sessionId={}", request.sessionId(), error);
                        sink.next(ServerSentEvent.<String>builder()
                                .event("error")
                                .data("服务暂时不可用，请稍后重试")
                                .build());
                        sink.complete();
                    })
                    .start());
        });
    }
}
