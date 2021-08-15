package com.hrbu.concurrent.future.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FutureUtils {

    public static <T> T get(Future<T> future) {
        try {
            return future == null ? null : future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }
}
