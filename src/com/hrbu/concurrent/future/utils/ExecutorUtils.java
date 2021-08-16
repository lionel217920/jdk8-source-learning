package com.hrbu.concurrent.future.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于Executor + Future的工具类
 */
public class ExecutorUtils {

    /**
     * 自定义的FutureTask的，同步执行被拒绝的任务
     *
     * @param <V>
     */
    public static class CallableFuture<V> implements Future<V>, Callable<V> {

        private V result;

        private Callable<V> callable;

        public CallableFuture(Callable<V> callable) {
            this.callable = callable;
        }

        @Override
        public V call() {
            try {
                return result = callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return result;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return result;
        }
    }

    public static <Q> List<Q> execute(ExecutorService executor, List<? extends Callable<Q>> taskList) {
        int size = taskList.size();
        List<Q> results = new ArrayList<>(size);
        if (size < 1) {
            return results;
        }

        List<CallableFuture<Q>> rejectedList = new ArrayList<>();
        List<Future<Q>> futureList = new ArrayList<>(size);
        for (final Callable<Q> callable : taskList) {
            try {
                Future<Q> future = executor.submit(callable);
                futureList.add(future);
            } catch (RejectedExecutionException e) {
                CallableFuture<Q> callFuture = new CallableFuture<>(callable);
                rejectedList.add(callFuture);
                futureList.add(callFuture);
            }
        }
        try {
            for (CallableFuture<Q> future : rejectedList) {
                System.out.println("被决绝任务在" + Thread.currentThread().getName() + "中执行。");
                future.call();
            }
            for (Future<Q> future : futureList) {
                results.add(FutureUtils.get(future));
            }
            return results;
        } catch (RuntimeException e) {
            for (Future<Q> future : futureList) {
                future.cancel(true);
            }
            throw e;
        }

    }
}
