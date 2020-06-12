package com.richard.library.clink.core;


import com.richard.library.clink.box.StringReceivePacket;
import com.richard.library.clink.box.StringSendPacket;
import com.richard.library.clink.impl.SocketChannelAdapter;
import com.richard.library.clink.impl.async.AsyncReceiveDispatcher;
import com.richard.library.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;


//用于连接
public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    private UUID key = UUID.randomUUID();

    //这个是最基本的channel
    private SocketChannel channel;

    //这里的sender和receiver是基于以上的channel的进行一个封装和基本的完善
    private Sender sender;
    private Receiver receiver;

    private SendDispatcher sendDispatcher;

    private ReceiveDispatcher receiveDispatcher;


    //这个是建立连接
    public void setup(SocketChannel socketChannel) throws IOException {

        this.channel = socketChannel;

        //得到IoContext的实例
        IoContext context = IoContext.get();

        //创建一个SocketChannelAdapter
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        this.sender = adapter;

        this.receiver = adapter;

        sendDispatcher = new AsyncSendDispatcher(sender);


        receiveDispatcher = new AsyncReceiveDispatcher(receiver,receivePacketCallback);

        //启动接收
        receiveDispatcher.start();
        //读下一条消息
        //readNextMessage();
    }

    public void send(String msg){
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

//    private void readNextMessage() {
//        if (receiver != null) {
//            try {
//                receiver.receiveAsync(echoReceiveListener);
//
//            } catch (IOException e) {
//                System.out.println("开始接收数据异常：" + e.getMessage());
//            }
//        }
//    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();

        channel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }


//    private IoArgs.IoArgsEventListener echoReceiveListener = new IoArgs.IoArgsEventListener() {
//        @Override
//        public void onStarted(IoArgs args) {
//
//        }
//
//        @Override
//        public void onCompleted(IoArgs args) {
//            // 打印
//            onReceiveNewMessage(args.bufferString());
//            // 读取下一条数据
//            readNextMessage();
//        }
//    };

    protected void onReceiveNewMessage(String str) {
        System.out.println(key.toString() + ":" + str);
    }


    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        //接收到数据的回调
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            if (packet instanceof StringReceivePacket){
                String msg = ((StringReceivePacket) packet).string();
                onReceiveNewMessage(msg);
            }
        }
    };
}
