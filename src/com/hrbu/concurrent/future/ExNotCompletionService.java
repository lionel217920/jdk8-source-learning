package com.hrbu.concurrent.future;

import com.hrbu.concurrent.future.utils.FutureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 不使用CompletionService的例子
 */
public class ExNotCompletionService {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private static final CompletionService<Integer> completionService = new ExecutorCompletionService<>(executorService);


    public List<Callable<Integer>> taskList() {
        List<Callable<Integer>> taskList = new ArrayList<>(5);

        for (int i = 5; i >= 1; i--) {
            int index = i;
            taskList.add(() -> {
                System.out.println("task" + index + " start");
                TimeUnit.SECONDS.sleep(index * 2L);
                System.out.println("task" + index + " end");
               return index;
            });
        }

        return taskList;
    }

    public List<Future<Integer>> calculateNotUse() {
        List<Future<Integer>> futureList = new ArrayList<>(5);

        for (Callable<Integer> callable : taskList()) {
            Future<Integer> future = executorService.submit(callable);
            futureList.add(future);
        }

        return futureList;
    }

    public List<Future<Integer>> calculateUse() {
        List<Future<Integer>> futureList = new ArrayList<>(5);


        for (Callable<Integer> callable : taskList()) {
            completionService.submit(callable);
        }

        for (int i = 0; i < 5; i++) {
            //futureList.add(completionService.poll());

            try {
                futureList.add(completionService.poll(1, TimeUnit.SECONDS));
                //futureList.add(completionService.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return futureList;
    }

    public static void main(String[] args) {
        ExNotCompletionService ex = new ExNotCompletionService();
        List<Future<Integer>> futureList = ex.calculateUse();

        for (Future<Integer> future : futureList) {
            Integer index = FutureUtils.get(future);
            System.out.println(index);
        }

        executorService.shutdown();
    }
}
