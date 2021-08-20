package com.hrbu.concurrent.future;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ExCompletableFutureStage {

    public static void thenApply() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            long result = new Random().nextInt(100);
            System.out.println("result1 = " + result);
            return result;
        }).thenApply(t -> {
            long result = t * 5;
            System.out.println("result2 = " + result);
            return result;
        });

        long result = future.get();
        System.out.println(result);
    }

    public static void thenApplyAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long result = new Random().nextInt(100);
            System.out.println("result1 = " + result);
            return result;
        }).thenApplyAsync(t -> {
            long result = t * 5;
            System.out.println("result2 = " + result);
            return result;
        });

        long result = future.get();
        System.out.println(result);
    }

    public static void whenComplete() throws Exception {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
            }

            if(new Random().nextInt() % 2 >= 0) {
                int i = 12/0;
            }
            System.out.println("run end ...");
        });

        future.whenComplete(new BiConsumer<Void, Throwable>() {
            @Override
            public void accept(Void t, Throwable action) {
                System.out.println("执行完成！");
            }

        });
        future.exceptionally(new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable t) {
                System.out.println("执行失败！"+t.getMessage());
                return null;
            }
        });

        TimeUnit.SECONDS.sleep(2);
    }

    public static void main(String[] args) throws Exception {
        //thenApply();
        //thenApplyAsync();
        whenComplete();
    }
}
