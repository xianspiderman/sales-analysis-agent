package com.dyh.salesAgent.controller;

import com.dyh.salesAgent.agent.SalesAgent;
import com.dyh.salesAgent.memory.MysqlChatMemoryStore;
import com.dyh.salesAgent.security.UserContext;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import reactor.core.Disposable;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserScopedControllerMemoryTest {

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void synchronousChatAndDeleteShouldUseCurrentUserMemoryId() {
        SalesAgent salesAgent = mock(SalesAgent.class);
        MysqlChatMemoryStore memoryStore = mock(MysqlChatMemoryStore.class);
        SalesAgentController controller = new SalesAgentController(salesAgent, memoryStore);
        UserContext.set(new UserContext.UserInfo(3L, "salesRep", "SALES_REP", 1L, 3L));
        when(salesAgent.chat(anyString(), anyString(), anyString())).thenReturn("回答");

        controller.chat(new ChatRequest("shared-session", "你好"));
        controller.clearSession("shared-session");

        verify(salesAgent).chat("3:shared-session", "你好", java.time.LocalDate.now().toString());
        verify(memoryStore).deleteMessages("3:shared-session");
    }

    @Test
    void streamingChatShouldUseCapturedUserMemoryId() {
        SalesAgent salesAgent = mock(SalesAgent.class);
        TokenStream tokenStream = mock(TokenStream.class, Answers.RETURNS_SELF);
        when(salesAgent.chatStream(anyString(), anyString(), anyString())).thenReturn(tokenStream);
        SalesAgentStreamController controller = new SalesAgentStreamController(salesAgent);
        UserContext.set(new UserContext.UserInfo(7L, "manager", "SALES_MANAGER", 2L, 7L));

        Disposable subscription = controller
                .chatStream(new ChatRequest("shared-session", "查询业绩"))
                .subscribe();

        verify(salesAgent).chatStream("7:shared-session", "查询业绩", java.time.LocalDate.now().toString());
        verify(tokenStream).start();
        subscription.dispose();
    }
}
