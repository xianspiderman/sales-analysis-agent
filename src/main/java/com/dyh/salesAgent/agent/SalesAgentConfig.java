package com.dyh.salesAgent.agent;
import com.dyh.salesAgent.memory.MysqlChatMemoryStore;
import com.dyh.salesAgent.security.UserContextStreamingChatModel;
import com.dyh.salesAgent.tool.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SalesAgentConfig {

    private final ChatModel chatLanguageModel;
    private final StreamingChatModel streamingChatModel;
    private final SalesQueryTool salesQueryTool;
    private final SalesSummaryTool salesSummaryTool;
    private final SalesTrendTool salesTrendTool;
    private final ChartGeneratorTool chartGeneratorTool;
    private final AnomalyDetectionTool anomalyDetectionTool;
    private final MysqlChatMemoryStore chatMemoryStore;

    @Bean
    public SalesAgent salesAgent() {
        return AiServices.builder(SalesAgent.class)
                .chatModel(chatLanguageModel)
                .streamingChatModel(new UserContextStreamingChatModel(streamingChatModel))
                .tools(salesQueryTool,
                       salesSummaryTool,
                       salesTrendTool,
                       chartGeneratorTool,
                       anomalyDetectionTool)
                .beforeToolExecution(exec ->
                        log.info("▶ 工具调用开始 | 工具：{} | 参数：{}",
                                exec.request().name(),
                                exec.request().arguments()))
                .afterToolExecution(exec ->
                        log.info("◀ 工具调用完成 | 工具：{} | 结果长度：{} 字符",
                                exec.request().name(),
                                exec.result() != null ? exec.result().length() : 0))
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(20)
                                .chatMemoryStore(chatMemoryStore)
                                .build())
                .build();
    }
}
