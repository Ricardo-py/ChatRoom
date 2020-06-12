package com.richard.Test.CompareAndSetTest;

import javax.swing.event.ListDataEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    private static AtomicBoolean isDone = new AtomicBoolean(false);

    public static void main(String[] args) throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(()->{
            countDownLatch.countDown();
            while (!isDone.compareAndSet(true,false)){
                System.out.println("thread");
            }
        }).start();

        countDownLatch.await();
        while (!isDone.compareAndSet(false,true)){
            System.out.println("main");
        }
    }
}
