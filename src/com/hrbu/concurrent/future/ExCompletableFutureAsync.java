package com.hrbu.concurrent.future;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * AsyncSupply、AsyncSupply Example
 */
public class ExCompletableFutureAsync {

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);

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
            if (value.equals(1)) {
                throw new RuntimeException();
            }
            System.out.println(name + " end " + value);
            return value;
        }

        @Override
        public void run() {
            System.out.println(name + " begin..");
            sleep();
            System.out.println(name + " end " + value);
        }

        private void sleep() {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void asyncRun() {
        for (int i = 0; i < 100; i++) {
            Calculate calculate = new Calculate(i);
            CompletableFuture<Void> future = CompletableFuture.runAsync(calculate);

//            try {
//                future.get();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            }
            future.join();
            //System.out.println(future.isDone());
        }
    }

    public void supplyAsync() {
        Integer totalCount = 0;

        CompletableFuture<Integer>[] futureList = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            Calculate calculate = new Calculate(i);
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(calculate, executorService);
            //totalCount += future.join();
            futureList[i] = future;
        }

        CompletableFuture.allOf(futureList).join();

        for (CompletableFuture<Integer> future : futureList) {
            totalCount += future.join();
        }

        System.out.println(totalCount);
    }

    public static void main(String[] args) {
        ExCompletableFutureAsync futureAsync = new ExCompletableFutureAsync();
        //futureAsync.asyncRun();
        futureAsync.supplyAsync();

        executorService.shutdown();
    }

}
