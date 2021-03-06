package com.richard.library.clink.impl;


import com.richard.library.clink.core.IoProvider;
import com.richard.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IoSelectorProvider implements IoProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // 是否处于某个过程
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);


    private final Selector readSelector;


    private final Selector writeSelector;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();

    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();


    private final ExecutorService inputHandlePool;

    private final ExecutorService outputHandlePool;



    public IoSelectorProvider() throws IOException {

        //创建Selector
        readSelector = Selector.open();

        writeSelector = Selector.open();

        inputHandlePool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Input-Thread-"));

        outputHandlePool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Output-Thread-"));
//        inputHandlePool = Executors.newSingleThreadExecutor();
//        outputHandlePool = Executors.newSingleThreadExecutor();

        // 开始输出输入的监听
        startRead();
        startWrite();
    }

    private void startRead() {
        Thread thread = new SelectThread("Clink IoSelectorProvider ReadSelector Thread",
                isClosed,inRegInput,readSelector,
                inputCallbackMap,inputHandlePool,SelectionKey.OP_READ);

        thread.start();
    }



    private void startWrite() {
        Thread thread = new SelectThread("Clink IoSelectorProvider WriteSelector Thread",
                isClosed,inRegOutput,writeSelector,
                outputCallbackMap,outputHandlePool,SelectionKey.OP_WRITE);
        thread.start();
    }


    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {

        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput,
                inputCallbackMap, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput,
                outputCallbackMap, callback) != null;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap,inRegInput);
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap,inRegOutput);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlePool.shutdown();
            outputHandlePool.shutdown();

            inputCallbackMap.clear();
            outputCallbackMap.clear();


            CloseUtils.close(readSelector, writeSelector);
        }
    }

    private static void waitSelection(final AtomicBoolean locker) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static SelectionKey registerSelection(SocketChannel channel, Selector selector,
                                                  int registerOps, AtomicBoolean locker,
                                                  HashMap<SelectionKey, Runnable> map,
                                                  Runnable runnable) {

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker) {
            // 设置锁定状态
            locker.set(true);

            try {
                // 唤醒当前的selector，让selector不处于select()状态，select是阻塞状态
                // 如果不唤醒selector，可能导致当前的socket的注册是无效的
                selector.wakeup();

                SelectionKey key = null;

                if (channel.isRegistered()) {
                    // 查询是否已经注册过
                    key = channel.keyFor(selector);
                    //如果已经注册过，就只需要添加监听状态就可以了
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                if (key == null) {
                    // 注册selector得到Key
                    key = channel.register(selector, registerOps);
                    // 注册回调
                    map.put(key, runnable);
                }

                return key;
            } catch (ClosedChannelException
                    | CancelledKeyException
                    | ClosedSelectorException e) {
                return null;
            } finally {
                // 解除锁定状态
                locker.set(false);
                try {
                    // 通知
                    locker.notify();
                } catch (Exception ignored) {

                }
            }
        }
    }

    private static void unRegisterSelection(SocketChannel channel, Selector selector,
                                            Map<SelectionKey, Runnable> map,
                                            AtomicBoolean locker) {
        synchronized (locker) {
            locker.set(true);
            selector.wakeup();
            try {
                if (channel.isRegistered()) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                        map.remove(key);
                    }
                }
            }finally {
                locker.set(false);
                try {
                    locker.notifyAll();
                }catch(Exception ignored){

                }
            }
        }
    }

    //这里才是真正的进行处理
    private static void handleSelection(SelectionKey key, int keyOps,
                                        HashMap<SelectionKey, Runnable> map,
                                        ExecutorService pool, AtomicBoolean locker) {
        // 重点
        // 取消继续对keyOps的监听
        //如果不这么做，将会导致处理线程的大量堆积
        synchronized (locker) {
            try {
                key.interestOps(key.readyOps() & ~keyOps);
            }catch (CancelledKeyException e){
                return;
            }
        }


        Runnable runnable = null;
        try {
            runnable = map.get(key);
        } catch (Exception ignored) {

        }

        if (runnable != null && !pool.isShutdown()) {
            // 异步调度
            pool.execute(runnable);
        }
    }

    static class SelectThread extends Thread{

        private final AtomicBoolean isClosed;
        private final AtomicBoolean locker;
        private final Selector selector;
        private final HashMap<SelectionKey, Runnable> callMap;
        private final ExecutorService pool;
        private final int keyOps;

        public SelectThread(String name,
                            AtomicBoolean isClosed, AtomicBoolean locker,
                            Selector selector,
                            HashMap<SelectionKey, Runnable> callMap,
                            ExecutorService pool, int keyOps) {
            super(name);
            this.isClosed = isClosed;
            this.locker = locker;
            this.selector = selector;
            this.callMap = callMap;
            this.pool = pool;
            this.keyOps = keyOps;
            this.setPriority(Thread.MAX_PRIORITY);
        }


        @Override
        public void run() {
            super.run();
            AtomicBoolean locker = this.locker;
            AtomicBoolean isClosed = this.isClosed;
            Selector selector = this.selector;
            HashMap<SelectionKey, Runnable> callMap = this.callMap;

            ExecutorService pool = this.pool;
            int keyOps = this.keyOps;

            while (!isClosed.get()) {
                try {
                    if (selector.select() == 0) {
                        waitSelection(locker);
                        continue;
                    }else if (locker.get()){
                        waitSelection(locker);
                    }

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator =  selectionKeys.iterator();
                    while (iterator.hasNext()){
                        SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isValid()){
                            handleSelection(selectionKey,
                                    keyOps,
                                    callMap,pool,locker);
                        }
                        iterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch(ClosedSelectorException ignored){
                    break;
                }
            }
        }

    }


    static class IoProviderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
