package com.hrbu.concurrent.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ExCompletableFutureStage {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.complete("Hello");
        CompletableFuture<String> newFuture = future.thenApplyAsync(s -> s + " World");

        System.out.println(newFuture.get());
    }
}
