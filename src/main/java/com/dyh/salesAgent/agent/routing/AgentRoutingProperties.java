package com.dyh.salesAgent.agent.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 统一入口的默认模式、请求覆盖和失败降级配置。 */
@Component
@ConfigurationProperties(prefix = "sales-agent.routing")
public class AgentRoutingProperties {

    private AgentRouteMode defaultMode = AgentRouteMode.AGENTSCOPE_TEAM;
    private AgentRouteMode fallbackMode = AgentRouteMode.LANGCHAIN4J;
    private boolean fallbackEnabled = true;
    private boolean allowRequestOverride = true;

    public AgentRouteMode getDefaultMode() { return defaultMode; }
    public void setDefaultMode(AgentRouteMode defaultMode) { this.defaultMode = defaultMode; }
    public AgentRouteMode getFallbackMode() { return fallbackMode; }
    public void setFallbackMode(AgentRouteMode fallbackMode) { this.fallbackMode = fallbackMode; }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
    public boolean isAllowRequestOverride() { return allowRequestOverride; }
    public void setAllowRequestOverride(boolean allowRequestOverride) { this.allowRequestOverride = allowRequestOverride; }
}
