package com.hrbu.concurrent.future.design;

/**
 * Future接口提供了获取计算和判断任务是否完成的两个接口
 * @param <T>
 */
public interface Future<T> {

    /**
     * 返回计算后的结果，改方法会陷入阻塞状态
     *
     * @return
     * @throws InterruptedException
     */
    T get() throws InterruptedException;

    /**
     * 判断任务是否已经被执行完成
     *
     * @return
     */
    boolean done();
}
