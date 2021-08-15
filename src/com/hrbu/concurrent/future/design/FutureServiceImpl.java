package com.hrbu.concurrent.future.design;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 主要作用是当提交任务时创建一个新的线程来受理任务，从而达到异步执行的效果
 *
 * @param <IN>
 * @param <OUT>
 */
public class FutureServiceImpl<IN, OUT> implements FutureService<IN, OUT> {

    // 为执行的线程指定名字前缀，为线程起一个特殊的名字是一个好的编码习惯
    private final static String FUTURE_THREAD_PREFIX = "future-";

    private final AtomicInteger nextCounter = new AtomicInteger(0);

    private String getNextName() {
        return FUTURE_THREAD_PREFIX + nextCounter.getAndIncrement();
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        final FutureTask<Void> futureTask = new FutureTask<>();
        new Thread(() -> {
            runnable.run();
            // 任务执行结束后将null作为结果传给future
            futureTask.finish(null);
        }, getNextName()).start();

        return futureTask;
    }

    @Override
    public Future<OUT> submit(Task<IN, OUT> task, IN input) {
        final FutureTask<OUT> futureTask = new FutureTask<>();
        new Thread(() -> {
            OUT result = task.get(input);
            // 任务执行结束后，将真实的结果通过finish方法传递给future
            futureTask.finish(result);
        }, getNextName()).start();

        return futureTask;
    }

    @Override
    public FutureTask<OUT> submit(Task<IN, OUT> task, IN input, Callback<OUT> callback) {
        final FutureTask<OUT> futureTask = new FutureTask<>();
        new Thread(() -> {
            OUT result = task.get(input);
            // 任务执行结束后，将真实的结果通过finish方法传递给future
            futureTask.finish(result);
            // 执行回调接口
            if (Objects.isNull(callback)) {
                callback.call(result);
            }
        }, getNextName()).start();

        return futureTask;
    }
}
