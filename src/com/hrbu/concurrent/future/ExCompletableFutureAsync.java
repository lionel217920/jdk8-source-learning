package com.hrbu.concurrent.future;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * AsyncSupply、AsyncSupply Example
 */
public class ExCompletableFutureAsync {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static class Calculate implements Supplier<Integer>, Runnable {

        private final String name;
        private final Integer value;

        public Calculate(Integer value) {
            this.name = "Task " + value;
            this.value = value;
        }

        @Override
        public Integer get() {
            System.out.println(name + " begin..");
            sleep();
            if (value.equals(5)) {
                throw new RuntimeException();
            }
            System.out.println(name + " end " + value);
            return value;
        }

        @Override
        public void run() {
            System.out.println(name + " begin..");
            sleep();
            if (value.equals(5)) {
                throw new RuntimeException();
            }
            System.out.println(name + " end " + value);
        }

        private void sleep() {
            try {
                TimeUnit.SECONDS.sleep(value);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void asyncRun() throws InterruptedException {
        Calculate calculate = new Calculate(20);
        CompletableFuture<Void> future = CompletableFuture.runAsync(calculate, executorService);
        System.out.println("async run other");

        for (int i = 0; i < 8; i++) {
            executorService.submit((Runnable) future::join);
        }

        TimeUnit.SECONDS.sleep(1);
        System.out.println("future dependents is " + future.getNumberOfDependents());

        try {
            future.join();
        } catch (CompletionException e) {
            System.out.println(e.getMessage());
        }
    }

    public void asyncRunWithCancel() throws InterruptedException {
        Calculate calculate = new Calculate(20);
        CompletableFuture<Void> future = CompletableFuture.runAsync(calculate, executorService);
        System.out.println("async run other");

        TimeUnit.SECONDS.sleep(5);
        System.out.println("future is cancelled " + future.isCancelled());
        boolean cancel = future.cancel(true);
        System.out.println("future cancel result " + cancel);

        try {
            future.join();
        } catch (CancellationException e) {

        }

        System.out.println(future.isCompletedExceptionally());
    }

    public void supplyAsync() {
        Integer totalCount = 0;
        for (int i = 0; i < 10; i++) {
            Calculate calculate = new Calculate(i);
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(calculate, executorService);
            totalCount += future.join();
        }

        System.out.println(totalCount);
    }

    public static void main(String[] args) throws InterruptedException {
        ExCompletableFutureAsync futureAsync = new ExCompletableFutureAsync();

        futureAsync.asyncRun();
        //futureAsync.asyncRunWithCancel();
        //futureAsync.supplyAsync();

        executorService.shutdown();
        System.out.println("main end");
    }

}
