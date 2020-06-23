package com.richard.library.clink.impl.async;

import com.richard.library.clink.core.IoArgs;
import com.richard.library.clink.core.SendDispatcher;
import com.richard.library.clink.core.SendPacket;
import com.richard.library.clink.core.Sender;
import com.richard.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor, AsyncPacketReader.PacketProvider {

    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);


    private final AsyncPacketReader reader = new AsyncPacketReader(this);
    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender){
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {

        synchronized (queueLock) {
            //将消息加到队列当中
            queue.offer(packet);

            //如果没有其他线程在发送消息我们就对消息进行发送
            if (isSending.compareAndSet(false, true)) {
                if (reader.requestTakePacket())
                    requestSend();
            }
        }
    }

    @Override
    public void cancel(SendPacket packet) {
        boolean ret;

        synchronized (queueLock){
            ret = queue.remove(packet);
        }

        if (ret){
            packet.cancel();
            return;
        }

        reader.cancel(packet);
    }

    //从队列中取packet
    @Override
    public SendPacket takePacket(){
        SendPacket packet;
        synchronized (queueLock){
            packet = queue.poll();
            if (packet == null){
                //队列为空，取消发送状态
                isSending.set(false);
                return null;
            }
        }

        //如果消息失效的话，继续取消息
        if (packet.isCanceled()){
            return takePacket();
        }
        return packet;
    }

    @Override
    public void completedPacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }


    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {

        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
            e.printStackTrace();
        }
    }


    private void closeAndNotify() {
        CloseUtils.close(this);
    }


    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false,true)){
            isSending.set(false);
            //reader关闭
            reader.close();
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return reader.fillData();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        if (args == null) {
            e.printStackTrace();
        }else{
            //TODO
        }
    }

    @Override
    public void oncConsumeCompleted(IoArgs args) {
        //继续发送当前包
        if (reader.requestTakePacket()){
            requestSend();
        }
    }

}
