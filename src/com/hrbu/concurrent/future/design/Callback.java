package com.hrbu.concurrent.future.design;

/**
 * 使用回调机制可以让调用者不再进行显示地通过get方法获取结果而导致阻塞
 * 类似于JDK8中的Consumer
 * @param <T>
 */
public interface Callback<T> {

    /**
     * 任务完成后会调用该方法
     *
     * @param t
     */
    void call(T t);
}
