package com.dyh.salesAgent.security;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.*;

import java.util.List;
import java.util.Set;

/**
 * StreamingChatModel 的装饰器：不改变模型能力，只在 LangChain4j 回调边界传播当前登录身份。
 * 同步 Agent 通常留在 MVC 请求线程；流式 Agent 的回调可能切到模型线程，所以只包装流式模型。
 */
// final防止再通过继承改变传播边界
public final class UserContextStreamingChatModel implements StreamingChatModel {

    // 真正负责访问大模型的原始 StreamingChatModel。
    private final StreamingChatModel delegate;

    public UserContextStreamingChatModel(StreamingChatModel delegate) {
        // 构造时保存原始模型，后续所有模型调用和元数据查询仍交给它完成。
        this.delegate = delegate;
    }

    @Override
    public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        UserContext.UserInfo snapshot = UserContext.requireCurrent();
        // 请求原样交给原模型；仅把原 handler 换成带上下文恢复能力的代理 handler。
        delegate.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // 每段文本到达时临时绑定 snapshot，再调用 LangChain4j 原处理器。
                UserContext.runWith(snapshot, () -> handler.onPartialResponse(partialResponse));
            }

            @Override
            public void onPartialResponse(PartialResponse response, PartialResponseContext context) {
                // 带响应上下文的文本分片重载也必须传播身份。
                UserContext.runWith(snapshot, () -> handler.onPartialResponse(response, context));
            }

            @Override
            public void onPartialThinking(PartialThinking thinking) {
                // 推理分片回调使用同一身份边界。
                UserContext.runWith(snapshot, () -> handler.onPartialThinking(thinking));
            }

            @Override
            public void onPartialThinking(PartialThinking thinking, PartialThinkingContext context) {
                // 带上下文的推理分片重载同样使用同一身份边界。
                UserContext.runWith(snapshot, () -> handler.onPartialThinking(thinking, context));
            }

            @Override
            public void onPartialToolCall(PartialToolCall toolCall) {
                // 模型逐步生成工具调用参数时传播身份。
                UserContext.runWith(snapshot, () -> handler.onPartialToolCall(toolCall));
            }

            @Override
            public void onPartialToolCall(PartialToolCall toolCall, PartialToolCallContext context) {
                // 带上下文的工具调用分片重载也传播身份。
                UserContext.runWith(snapshot, () -> handler.onPartialToolCall(toolCall, context));
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall toolCall) {
                // 完整工具调用交回 LangChain4j 时身份已绑定，随后执行的 @Tool 能读取正确用户。
                UserContext.runWith(snapshot, () -> handler.onCompleteToolCall(toolCall));
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                // 最终响应回调也在正确身份下执行。
                UserContext.runWith(snapshot, () -> handler.onCompleteResponse(response));
            }

            @Override
            public void onError(Throwable error) {
                // 错误路径同样恢复身份，并由 runWith 的 finally 保证清理。
                UserContext.runWith(snapshot, () -> handler.onError(error));
            }
        });
    }

    // 以下模型元数据不涉及权限，只需原样委托，保证装饰前后的模型行为一致。
    @Override public ChatRequestParameters defaultRequestParameters() { return delegate.defaultRequestParameters(); }
    @Override public List<ChatModelListener> listeners() { return delegate.listeners(); }
    @Override public ModelProvider provider() { return delegate.provider(); }
    @Override public Set<Capability> supportedCapabilities() { return delegate.supportedCapabilities(); }
}
