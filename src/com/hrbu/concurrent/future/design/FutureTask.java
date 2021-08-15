package com.hrbu.concurrent.future.design;

/**
 * Future接口的实现
 * @param <T>
 */
public class FutureTask<T> implements Future<T> {

    // 计算结果
    private T result;

    // 任务是否完成
    private boolean isDone = false;

    // 定义锁
    private final Object LOCK = new Object();

    @Override
    public T get() throws InterruptedException {
        synchronized (LOCK) {
            // 当任务还没完成时，调用get方法会被挂起而进入阻塞
            while (!isDone) {
                LOCK.wait();
            }

            // 返回计算结果
            return result;
        }
    }

    protected void finish(T result) {
        synchronized (LOCK) {
            if (isDone) {
                return;
            }

            // 计算完成，为result指定结果，并且将isDone设置为true，
            this.result = result;
            this.isDone = true;
            // 利用线程间的通信,知道任务完成唤醒阻塞线程
            LOCK.notifyAll();
        }
    }

    @Override
    public boolean done() {
        return isDone;
    }
}
