package com.dyh.salesAgent.config;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.dyh.salesAgent.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.auth.enabled:true}")
    private boolean authEnabled;

    @Value("${app.auth.dev.user-id:13}") private Long devUserId;
    @Value("${app.auth.dev.username:本地开发总监}") private String devUsername;
    @Value("${app.auth.dev.role:SALES_DIRECTOR}") private String devRole;
    @Value("${app.auth.dev.region-id:1}") private Long devRegionId;
    @Value("${app.auth.dev.rep-id:13}") private Long devRepId;

    @Override
    public void addInterceptors(InterceptorRegistry registry) { // 往web项目里增加拦截器
        if (!authEnabled) {
            log.warn(">>> 权限校验已关闭（app.auth.enabled=false），仅限开发测试使用 <<<");
            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler) {
                    UserContext.set(new UserContext.UserInfo(
                            devUserId, devUsername, devRole, devRegionId, devRepId));
                    return true;
                }

                @Override
                public void afterCompletion(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Object handler, Exception ex) {
                    UserContext.clear();// 请求完成后清理 ThreadLocal，防止内存泄漏
                }
            }).addPathPatterns("/**");
            return;
        }

        // 以下是正常的权限拦截器逻辑（auth.enabled=true 时生效）
        // Sa-Token 登录校验拦截器——白名单之外的接口都需要登录
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) throws Exception {
                if (StpUtil.isLogin()) return true;
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"NOT_LOGIN\",\"message\":\"请先登录后再访问\"}");
                return false;
            }
        })
                .addPathPatterns("/**") // 无论任何请求都校验有没有token
                .excludePathPatterns(
                        "/auth/login", // 排除登录的
                        "/actuator/**", // 排除监控的
                        "/static/**"
                );

        // 用户上下文填充拦截器——从 Sa-Token Session 读取用户信息写入 ThreadLocal，都是固定写法
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) {
                if (StpUtil.isLogin()) { // 判断是否登录了
                    Long userId    = StpUtil.getLoginIdAsLong();
                    SaSession session = StpUtil.getSession();
                    String username = (String) session.get("username");
                    String role     = (String) session.get("role");
                    Long regionId   = session.get("regionId") instanceof Number n ? n.longValue() : null;
                    Long repId      = session.get("repId")    instanceof Number n ? n.longValue() : null;
                    UserContext.set(new UserContext.UserInfo(userId, username, role, regionId, repId));
                    log.debug("用户已认证: userId={}, role={}", userId, role);
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Object handler, Exception ex) {
                UserContext.clear();   // 请求完成后清理 ThreadLocal，防止内存泄漏
            }
        }).addPathPatterns("/**");
    }
}
