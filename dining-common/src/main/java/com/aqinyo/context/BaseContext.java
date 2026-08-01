package com.aqinyo.context;

/*
 *   BaseContext (上下文工具类)  -->  一句话总结: 它是 "当前登录用户ID" 的全局存取器
 *   核心机制: 基于 ThreadLocal 封装的, 让同一个请求线程内的所有代码都能随时拿到当前用户ID，不用一层层传参
 *   (请求进来)拦截器存、业务层取、(请求完成)拦截器清
 */
public class BaseContext {

    // 使用ThreadLocal存储当前线程的用户ID,保证线程隔离。
    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();


    // 存储 当前线程的用户ID
    public static void setCurrentId(Long id) {
        threadLocal.set(id); // setCurrentId:通常在拦截器（Interceptor）或过滤器（Filter）中，解析Token获取用户ID后调用，将ID存入当前线程
    }


    // 获取 当前线程的用户ID
    public static Long getCurrentId() {
        return threadLocal.get(); // getCurrentId:在业务逻辑层调用，获取当前登录用户的ID，用于数据权限校验、自动填充创建人/更新人等
    }


    // 移除 当前线程的用户ID  (注意: 必须在请求结束时调用，防止线程池复用导致的数据混乱和内存泄漏)
    public static void removeCurrentId() {
        threadLocal.remove(); // removeCurrentId:必须在请求结束时（如拦截器的 afterCompletion 方法中）调用，清除 ThreadLocal 中的数据
    }

}
