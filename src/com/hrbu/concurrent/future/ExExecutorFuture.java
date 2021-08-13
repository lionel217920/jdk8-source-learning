package com.hrbu.concurrent.future;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 多任务计算示例
 */
public class ExExecutorFuture {

    private static ExecutorService executorService = Executors.newFixedThreadPool(2);

    public static Future<Integer> calculate(ComputeTask workThread) {
        return executorService.submit(workThread);
    }

    public class ComputeTask implements Callable<Integer> {

        private Integer result;
        private String taskName;

        public ComputeTask(Integer result, String taskName) {
            this.result = result;
            this.taskName = taskName;
            System.out.println("生成子线程计算任务 " + taskName);
        }

        public Integer getResult() {
            return result;
        }

        public void setResult(Integer result) {
            this.result = result;
        }

        @Override
        public Integer call() throws Exception {
            for (int i = 0; i < 100; i++) {
                result = +i;
            }
            TimeUnit.SECONDS.sleep(10);

            System.out.println("子线程计算任务: " + taskName + " 执行完成!");
            return result;
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExExecutorFuture exExecutorFuture = new ExExecutorFuture();
        List<Future<Integer>> taskList = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            ComputeTask computeTask = exExecutorFuture.new ComputeTask(i, "Task" + i);
            Future<Integer> futureTask = executorService.submit(computeTask);
            taskList.add(futureTask);
        }

        for (int i = 0; i < 5; i++) {
            System.out.println("所有计算任务提交完毕, 主线程接着干其他事情！");
        }

        Integer totalResult = 0;
        for (Future<Integer> future : taskList) {
            totalResult += future.get();
        }

        System.out.println("多任务计算后的总结果是:" + totalResult);

        executorService.shutdown();
    }
}
