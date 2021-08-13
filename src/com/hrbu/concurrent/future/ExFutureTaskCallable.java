package com.hrbu.concurrent.future;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * FutureTask + Callable
 * FutureTask在高并发场景下只执行一次。
 */
public class ExFutureTaskCallable implements Callable<String> {

    private final int count = 20;

    private static ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public String call() throws Exception {
        System.out.println("开始执行了，在 " + Thread.currentThread().getName());
        // 执行后睡眠1分钟
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + " 被打断了");
        }
        for (int i = count; i > 0; i--) {
			//Thread.yield();
            System.out.println(Thread.currentThread().getName() + "当前票数：" + i);
        }
        return "sale out";
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Callable<String> callable = new ExFutureTaskCallable();
        FutureTask<String> futureTask = new FutureTask<>(callable);

        Thread mThread1 = new Thread(futureTask);
        Thread mThread2 = new Thread(futureTask);
        Thread mThread3 = new Thread(futureTask);

        mThread1.start();
        mThread2.start();
        mThread3.start();

        // main线程休眠20秒后取消任务
        TimeUnit.SECONDS.sleep(20);
        System.out.println("Task is cancelled " + futureTask.isCancelled());
        boolean cancelled = futureTask.cancel(true);
        System.out.println("Task cancelled result is " + cancelled);

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            executorService.execute(() -> {
                try {
                    if (finalI == 4) {
                        System.out.println(futureTask.get(10, TimeUnit.SECONDS));
                    } else {
                        System.out.println(futureTask.get());
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                }
            });
        }

        executorService.shutdown();
    }
}
