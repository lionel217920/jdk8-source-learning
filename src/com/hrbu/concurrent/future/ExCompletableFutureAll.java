package com.hrbu.concurrent.future;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ExCompletableFutureAll {

    private final static ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final Random random = new Random(5);

    public class Cal implements Runnable, Supplier<Integer> {

        private final Integer val;

        public Cal(Integer val) {
            this.val = val;
        }

        @Override
        public Integer get() {
            System.out.println("supplyAsync start in " + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(val);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("supplyAsync end in " + Thread.currentThread().getName());
            return val;
        }

        @Override
        public void run() {
            System.out.println("runAsync start in " + Thread.currentThread().getName());

            if (val == 3) {
                throw new RuntimeException("exception in 3");
            }

            try {
                TimeUnit.SECONDS.sleep(random.nextInt(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("runAsync end in" + Thread.currentThread().getName());
        }
    }

    public void allOf() {
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(new Cal(1), executorService);
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(new Cal(2), executorService);
        CompletableFuture<Void> future3 = CompletableFuture.runAsync(new Cal(6), executorService);
        CompletableFuture<Void> future4 = CompletableFuture.runAsync(new Cal(4), executorService);
        CompletableFuture<Void> future5 = CompletableFuture.runAsync(new Cal(5), executorService);

        CompletableFuture<Integer> future6 = CompletableFuture.supplyAsync(new Cal(13), executorService);
        CompletableFuture<Integer> future7 = CompletableFuture.supplyAsync(new Cal(18), executorService);
        CompletableFuture<Integer> future8 = CompletableFuture.supplyAsync(new Cal(16), executorService);
        CompletableFuture<Integer> future9 = CompletableFuture.supplyAsync(new Cal(15), executorService);
        CompletableFuture<Integer> future10 = CompletableFuture.supplyAsync(new Cal(10), executorService);

        //CompletableFuture<Object> future = CompletableFuture.anyOf(future1, future2, future3, future4, future5);
        //CompletableFuture<Void> future = CompletableFuture.allOf(future1, future2, future3, future4, future5);

        CompletableFuture<Object> future = CompletableFuture.anyOf(future9, future6, future7, future8, future10);
        System.out.println("future join " + future.join());

        System.out.println("allOf end ");
    }

    public static void main(String[] args) {
        ExCompletableFutureAll completableFuture = new ExCompletableFutureAll();
        completableFuture.allOf();

        executorService.shutdown();
    }
}
