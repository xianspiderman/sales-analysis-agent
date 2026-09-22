package com.dyh.salesAgent;
import com.dyh.salesAgent.agent.SalesAgent;
import com.dyh.salesAgent.memory.UserScopedMemoryId;
import com.dyh.salesAgent.security.UserContext;
import com.dyh.salesAgent.service.SalesQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class SalesAgentSmokeTest {

    @Autowired
    private SalesAgent salesAgent;

    @Autowired
    private SalesQueryService salesQueryService;

    // chat() 的第三个参数 today 对应 System Prompt 里的 {{today}} 变量
    private final String today = LocalDate.now().toString();

    @BeforeEach
    void setUpUserContext() {
        UserContext.set(new UserContext.UserInfo(
                13L, "sales_director", "SALES_DIRECTOR", null, null));
    }

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void smokeTest() {
        String response = salesAgent.chat(
                UserScopedMemoryId.fromCurrentUser("test-session-001"),
                "你好，你能做什么？",
                today);
        assertFalse(response.isBlank());
        System.out.println("Agent 回答：" + response);
    }

    @Test
    void toolCallTest() {
        assertFalse(salesQueryService.queryMonthlyTrend(
                UserContext.requireDataScope(), null, 6).isEmpty());
        String response = salesAgent.chat(
                UserScopedMemoryId.fromCurrentUser("test-session-002"),
                "近6个月的月度销售趋势是什么？",
                today);
        assertFalse(response.isBlank());
        System.out.println("Agent 回答：" + response);
    }
}
