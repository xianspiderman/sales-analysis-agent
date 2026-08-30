package com.dyh.salesAgent.security;

import java.util.function.Supplier;

public class UserContext {
    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    public record UserInfo(Long userId, String username, String role, Long regionId, Long repId) {} // repId是销售员id

    public static void set(UserInfo info) { HOLDER.set(info); }
    public static UserInfo get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    public static UserInfo requireCurrent() {
        UserInfo user = get(); // 从当前线程的ThreadLocal中读取身份，先保存在局部变量中
        if (user == null) {
            throw new IllegalStateException("用户上下文缺失，拒绝访问销售数据");
        }
        return user;
    }

    public static DataScope requireDataScope() {
        return DataScope.from(requireCurrent());
    }// 从已认证身份构造公司、大区或个人范围

    // 这是一个“在指定用户身份下运行一段代码”的工具方法；它只执行动作，不返回业务结果，所以返回类型是void。
    public static void runWith(UserInfo user, Runnable action) {
        // 保存“当前回调线程”进入本方法前的身份；嵌套调用时不能把外层身份弄丢。
        UserInfo previous = HOLDER.get(); // 先读取模型回调线程B原来的身份。通常是null，但保存它可以支持runWith嵌套。
        // try 保证 action 正常结束或抛异常时都会进入 finally。
        try {
            // 把发起 SSE 请求时捕获的身份临时绑定到当前模型回调线程。
            set(user);
            // 在身份已经绑定的时间窗口内执行真正的回调、Agent 工具或完成/错误处理。
            action.run();
        } finally {
            // 进入本方法前没有身份，说明本次临时绑定结束后必须 remove，防止线程池复用时串号。
            if (previous == null) {
                clear();
            } else {
                // 进入本方法前已有身份时恢复它，使 runWith 可以安全嵌套。
                set(previous);
            }
        }
    }

    /**
     * 在指定身份下执行有返回值的动作。
     * AgentScope 工具通过 RuntimeContext 获取身份后，用此方法桥接尚未解耦的旧工具实现。
     */
    public static <T> T callWith(UserInfo user, Supplier<T> action) {
        UserInfo previous = HOLDER.get();
        try {
            set(user);
            return action.get();
        } finally {
            if (previous == null) {
                clear();
            } else {
                set(previous);
            }
        }
    }

    public static boolean isDirector() { // 判断是不是领导
        UserInfo u = get();
        return u != null && "SALES_DIRECTOR".equals(u.role());
    }

    public static boolean isManager() { // 判断是不是经理
        UserInfo u = get();
        return u != null && "SALES_MANAGER".equals(u.role());
    }
}
