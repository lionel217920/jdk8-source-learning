package com.hrbu.concurrent.future.design;

import java.util.concurrent.TimeUnit;

/**
 * Future设计模式
 */
public class FutureDesign {

    public static void main(String[] args) throws InterruptedException {
        FutureDesign futureDesign = new FutureDesign();
        //futureDesign.submit();
        //futureDesign.submitWithReturn();
        futureDesign.submitWithCallback();;
    }


    public void submit() throws InterruptedException {
        FutureService<Void, Void> service = FutureService.newService();

        Future<?> future = service.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("finish done.");
        });

        System.out.println("do something else.");

        // 当前main线程进入阻塞
        future.get();
    }

    public void submitWithReturn() throws InterruptedException {
        FutureService<String, Integer> service = FutureService.newService();

        Future<Integer> future = service.submit(input -> {
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("finish done.");
            return input.length();
        }, "Hello");

        System.out.println("do something else.");

        // 当前main线程进入阻塞
        Integer length = future.get();
        System.out.println("计算完成，长度：" + length);
    }

    public void submitWithCallback() {
        FutureService<String, Integer> service = FutureService.newService();

        Future<Integer> future = service.submit(input -> {
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("finish done.");
            return input.length();
        }, "Hello", System.out::println);


        System.out.println("do something else.");
    }
}
