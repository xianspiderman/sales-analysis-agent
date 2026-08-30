package com.dyh.salesAgent.agent.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/** AgentScope Java 销售分析 Agent 的独立配置，不影响现有 LangChain4j Bean。 */
@Configuration
public class AgentScopeSalesAgentConfig {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的销售数据分析助手，服务于销售团队。

            你可以查询销售订单、统计销售额和排名、分析同比环比趋势、生成 ECharts 图表数据、检测销售异常。
            必须通过工具获取销售事实数据，不得编造金额、排名、订单、产品或大区数据。
            工具已经根据当前登录用户执行公司、大区或销售员级别的数据权限隔离，不要尝试绕过权限范围。

            限制：
            - 只能查询，不能修改销售数据。
            - 不能预测未来销售。
            - 不能发送邮件、通知或执行其他外部操作。

            回答要求：
            - 使用中文，金额格式化为 ¥X,XXX。
            - 有数据时给出简短判断，不要只罗列结果。
            - 发现异常时主动提醒。
            - 工具返回 CHART_JSON: 时，必须原样保留完整内容，不要用代码块包裹。
            """;

    @Bean
    public Model agentScopeDashScopeModel(
            @Value("${DASHSCOPE_API_KEY}") String apiKey,
            @Value("${sales-agent.agentscope.model-name:qwen-max}") String modelName,
            @Value("${sales-agent.agentscope.temperature:0.1}") double temperature,
            @Value("${sales-agent.agentscope.max-tokens:2048}") int maxTokens) {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(true)
                .defaultOptions(GenerateOptions.builder()
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build())
                .build();
    }

    @Bean
    public AgentStateStore agentScopeStateStore(
            DataSource dataSource,
            @Value("${sales-agent.agentscope.state.initialize-schema:true}") boolean initializeSchema) {
        return new MysqlAgentStateStore(dataSource, initializeSchema);
    }

    @Bean(destroyMethod = "close")
    public ReActAgent agentScopeSalesAgent(
            Model agentScopeDashScopeModel,
            AgentStateStore agentScopeStateStore,
            AgentScopeSalesTools salesTools,
            AgentScopeObservabilityMiddleware observabilityMiddleware,
            @Value("${sales-agent.agentscope.max-iters:10}") int maxIters) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(salesTools);

        return ReActAgent.builder()
                .name("sales-analysis-agent")
                .sysPrompt(SYSTEM_PROMPT)
                .model(agentScopeDashScopeModel)
                .toolkit(toolkit)
                .middleware(observabilityMiddleware)
                .stateStore(agentScopeStateStore)
                .maxIters(maxIters)
                .build();
    }
}
