package com.hrbu.concurrent.thread;

public class ExExtendThread extends Thread {

    @Override
    public void run() {
        System.out.println("thread" + Thread.currentThread().getName());
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("run end");
    }

    public static void main(String[] args) {
        new ExExtendThread().start();
        System.out.println("main end");
    }

}
