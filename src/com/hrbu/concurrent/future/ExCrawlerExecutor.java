package com.hrbu.concurrent.future;

import com.hrbu.concurrent.future.utils.ExecutorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 使用ExecutorUtils的示例
 */
public class ExCrawlerExecutor {

    private static final ExecutorService executor = new ThreadPoolExecutor(2, 5, 0L,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(50));

    public static <Q> List<Q> execute(List<Callable<Q>> taskList) {
        return ExecutorUtils.execute(executor, taskList);
    }

    public static void main(String[] args) {
        List<Callable<Integer>> callableList = new ArrayList<>(100);

        for (int i = 0; i < 100; i++) {
            callableList.add(() -> {
                TimeUnit.SECONDS.sleep(1);
                return 1;
            });
        }

        List<Integer> executeResult = execute(callableList);
        Integer totalCount = 0;
        for (Integer value : executeResult) {
            totalCount += value;
        }

        System.out.println(totalCount);
        executor.shutdown();
    }
}
