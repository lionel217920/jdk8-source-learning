package com.hrbu.concurrent.future.design;

/**
 * Task接口提供给调用者实现计算逻辑，类似于JDK中的Callable接口
 */
@FunctionalInterface
public interface Task<IN, OUT> {

    /**
     * 给定一个参数，返回计算结果
     *
     * @param input
     * @return
     */
    OUT get(IN input);
}
