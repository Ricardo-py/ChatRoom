package com.richard.library.clink.impl;



import com.richard.library.clink.core.IoArgs;
import com.richard.library.clink.core.IoProvider;
import com.richard.library.clink.core.Receiver;
import com.richard.library.clink.core.Sender;
import com.richard.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements
        Sender, Receiver, Cloneable {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;

    private IoArgs receiveArgsTemp;

    //这里传入的listener就是Connector
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventListener receiveIoEventListener;

    private IoArgs.IoArgsEventListener sendIoEventListener;

    @Override
    public boolean receiveAsync(IoArgs args) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        receiveArgsTemp = args;
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public boolean setReceiveListener(IoArgs.IoArgsEventListener listener) {
        //这里在AsyncReceiveDispatcher中设置AsyncReceiveDispatcher中的ioArgsEventListener
        receiveIoEventListener = listener;
        return true;
    }

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;

        //这里的listener是connector
        this.listener = listener;

        channel.configureBlocking(false);
    }


//    @Override
//    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {
//        if (isClosed.get()) {
//            throw new IOException("Current channel is closed!");
//        }
//
//        //这里传入的listener是Connector中的echoReceiveListener
//        //inputCallback里面执行的就是receiveIoEventListener中的方法
//        receiveIoEventListener = listener;
//        return ioProvider.registerInput(channel, inputCallback);
//
//    }

    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        sendIoEventListener = listener;
        // 当前发送的数据附加到回调中
        outputCallback.setAttach(args);

        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            // 关闭
            CloseUtils.close(channel);

            // 回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }
    }


    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }

            IoArgs args = receiveArgsTemp;

            //设置的是AsyncReceiveDispatcher中的ioArgsEventListener
            IoArgs.IoArgsEventListener listener = SocketChannelAdapter.this.receiveIoEventListener;
            listener.onStarted(args);

            try {
                if (args.readFrom(channel) > 0){
                    listener.onCompleted(args);
                }else
                    throw new IOException("Cannot readFrom any data!");
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
                e.printStackTrace();
            }
//            try {
//                // 具体的读取操作
//                int read_size = args.read(channel);
//                System.out.println(read_size);
//                if (read_size > 0 && listener != null) {
//                    // 读取完成回调
//                    listener.onCompleted(args);
//                } else {
//                    throw new IOException("Cannot read any data!");
//                }
//            } catch (IOException ignored) {
//                CloseUtils.close(SocketChannelAdapter.this);
//            }


        }
    };


    //这里面的代码是添加到线程池中运行的，需要保证线程安全
    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {
            if (isClosed.get()) {
                return;
            }
            IoArgs args = getAttach();
            IoArgs.IoArgsEventListener listener = sendIoEventListener;
            listener.onStarted(args);

            try {
                if (args.writeTo(channel) > 0){
                    listener.onCompleted(args);
                }else
                    throw new IOException("Cannot Write any data!");
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
                e.printStackTrace();
            }

            // TODO
           // sendIoEventListener.onCompleted(null);
        }
    };


    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
