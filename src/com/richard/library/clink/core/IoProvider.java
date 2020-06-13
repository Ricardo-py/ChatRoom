package com.richard.library.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

/**
 * IoProvider针对所有连接的，所有的连接都可以用IoProvider进行注册和解除注册
 */
public interface IoProvider extends Closeable {

    //注册输入，想要从socketChannel中读取数据，但是是异步的模式，所以采用注册，与取消注册，观察者的模式
    boolean registerInput(SocketChannel channel, HandleInputCallback callback);

    //在我们注册的时候，并不知道socketChannel是否允许发数据，但是此时我们是有数据的，我们先暂时将数据附加到attach上面
    //等到可以发数据了的时候再回调发数据
    boolean registerOutput(SocketChannel channel, HandleOutputCallback callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);


    abstract class HandleInputCallback implements Runnable {

        //这个方法是用来给线程调度的方法
        @Override
        public final void run() {
            canProviderInput();
        }

        //线程运行的时候会调用抽象方法canProviderInput，当前我们能够提供输入
        protected abstract void canProviderInput();

    }

    abstract class HandleOutputCallback implements Runnable {

        //副本

        @Override
        public final void run() {
            canProviderOutput();
        }
        protected abstract void canProviderOutput();

    }

}
