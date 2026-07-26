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
@RequiredArgsConstructor // Lombok的final类构造方法注解，通过构造方法注入
@Slf4j
public class SalesAgentConfig {

    private final ChatModel chatLanguageModel;
    private final StreamingChatModel streamingChatModel;
    private final SalesQueryTool salesQueryTool;//工具1，具体数据
    private final SalesSummaryTool salesSummaryTool;//工具2，数据总结
    private final SalesTrendTool salesTrendTool;//工具3，趋势分析
    private final ChartGeneratorTool chartGeneratorTool;//工具4，画图
    private final AnomalyDetectionTool anomalyDetectionTool;//工具5，异常数据预警
    private final MysqlChatMemoryStore chatMemoryStore;   // 注入持久化存储

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
//                .chatMemoryProvider(memoryId ->MessageWindowChatMemory.withMaxMessages(20)) // 先用内存测试
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(20)
                                .chatMemoryStore(chatMemoryStore)
                                .build())
                .build();
    }
}
