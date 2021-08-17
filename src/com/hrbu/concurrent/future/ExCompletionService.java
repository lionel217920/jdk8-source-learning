package com.hrbu.concurrent.future;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CompletionService文档中的示例
 */
public class ExCompletionService {

    private static ExecutorService executorService = Executors.newFixedThreadPool(2);

    public void solve(Executor e, Collection<Callable<Result>> solvers) throws InterruptedException, ExecutionException {
        CompletionService<Result> ecs = new ExecutorCompletionService<>(e);

        for (Callable<Result> callable : solvers) {
            ecs.submit(callable);
        }

        int n = solvers.size();
        for (int i = 0; i < n; ++i) {
            Result r = ecs.take().get();
            if (r != null) {
                use(r);
            }
        }
    }

    public void use(Result result) {
        System.out.println(result.getName());
    }

    public static class Result {

        private final String name;
        private final Integer index;

        public String getName() {
            return name;
        }

        public Result(String name, Integer index) {
            this.name = name + index;
            this.index = index;
        }

        public void sleep() {
            try {
                TimeUnit.SECONDS.sleep(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void prepareSolvers() throws ExecutionException, InterruptedException {
        List<Callable<Result>> solvers = new ArrayList<>(5);

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            Callable<Result> solver = () -> {
                Result result = new Result("Result", finalI);
                result.sleep();
                return result;
            };
            solvers.add(solver);
        }

        solve(executorService, solvers);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExCompletionService exCompletionService = new ExCompletionService();
        exCompletionService.prepareSolvers();

        executorService.shutdown();
    }
}
