package com.richard.library.clink.core;


import com.richard.library.clink.box.BytesReceivePacket;
import com.richard.library.clink.box.FileReceivePacket;
import com.richard.library.clink.box.StringReceivePacket;
import com.richard.library.clink.box.StringSendPacket;
import com.richard.library.clink.impl.SocketChannelAdapter;
import com.richard.library.clink.impl.async.AsyncReceiveDispatcher;
import com.richard.library.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;


//用于连接
public abstract class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    protected UUID key = UUID.randomUUID();

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
    }


    public void send(String msg){
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }


    public void send(SendPacket packet){
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



    protected void onReceivePacket(ReceivePacket packet) {
        //System.out.println(key.toString() + ":[New Packet]-Type:" + packet.type() + ", length:" + packet.length);
    }

    protected abstract File createNewReceiveFile();

    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
            switch (type){
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length,createNewReceiveFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupported packet type");
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            onReceivePacket(packet);
        }
    };
}
