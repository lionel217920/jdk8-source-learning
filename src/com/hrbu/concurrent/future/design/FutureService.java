package com.hrbu.concurrent.future.design;

/**
 * FutureService主要用于提交任务
 */
public interface FutureService<IN, OUT> {

    /**
     * 提交不需要返回值的任务，Future的get方法返回null
     *
     * @param runnable
     * @return
     */
    Future<?> submit(Runnable runnable);

    /**
     * 提交需要返回值的任务，Task接口替代了Runnable接口
     *
     * @param task
     * @param input
     * @return
     */
    Future<OUT> submit(Task<IN, OUT> task, IN input);

    /**
     * 提交需要返回值的任务,并支持回调
     * 当提交的任务执行完成后，会将结果传递给Callback接口进行下一步的执行，不会因为get方法获取而陷入阻塞
     *
     * @param task
     * @param input
     * @param callback
     * @return
     */
    FutureTask<OUT> submit(Task<IN, OUT> task, IN input, Callback<OUT> callback);

    /**
     * 使用静态方法创建一个FutureService的实现
     *
     * @param <IN>
     * @param <OUT>
     * @return
     */
    static <IN, OUT> FutureService<IN, OUT> newService() {
        return new FutureServiceImpl<>();
    }
}
