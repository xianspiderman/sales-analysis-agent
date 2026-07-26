package com.dyh.salesAgent.config;
// TokenUsageLogger 实现了 ModelListener 接口，在 Spring 容器里声明为 @Component 后，langchain4j-open-ai-spring-boot-starter 会自动把它注册到 model 上，不需要手动在 SalesAgentConfig 里引用：
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TokenUsageLogger implements ChatModelListener {

    private final Counter inputTokenCounter; // 专门做统计的
    private final Counter outputTokenCounter;

    public TokenUsageLogger(MeterRegistry meterRegistry) {
        this.inputTokenCounter = Counter.builder("llm.tokens.input") // 自定义一个llm.tokens.input指标
                .description("Input tokens consumed") // 给它一个描述
                .register(meterRegistry); // 专门算输入了多少token，后面有用
        this.outputTokenCounter = Counter.builder("llm.tokens.output")
                .description("Output tokens consumed")
                .register(meterRegistry);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        var usage = responseContext.chatResponse().tokenUsage();
        if (usage != null) {
            int input = usage.inputTokenCount() != null ? usage.inputTokenCount() : 0; // 输入token数量
            int output = usage.outputTokenCount() != null ? usage.outputTokenCount() : 0; // 输出token数量

            inputTokenCounter.increment(input);
            outputTokenCounter.increment(output);

            // 估算费用（qwen-max 价格：输入 0.04 元/千Token，输出 0.12 元/千Token）
            double cost = input * 0.04 / 1000.0 + output * 0.12 / 1000.0;
            log.info("Token 用量 | 输入：{} | 输出：{} | 本次费用约：¥{}",
                    input, output, String.format("%.4f", cost));
        }
    }
}
