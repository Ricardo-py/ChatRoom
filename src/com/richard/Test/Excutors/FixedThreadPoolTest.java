package com.richard.Test.Excutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FixedThreadPoolTest {
    public static void main(String[] args){
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        executorService.execute(new A());
        executorService.execute(new B());
    }

    static class A implements Runnable{
        @Override
        public void run() {
            for (int i = 0; i < 100; i ++) {
                System.out.println("A:" + i);
            }
        }
    }

    static class B implements Runnable{

        @Override
        public void run() {
            for (int i = 0; i < 100; i ++)
                System.out.println("B:" + i);
        }
    }
}
