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

    public AsyncSendDispatcher(Sender sender){
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {

            //将消息加到队列当中
            queue.offer(packet);
            requestSend();
    }

    @Override
    public void cancel(SendPacket packet) {
        boolean ret;

        ret = queue.remove(packet);

        if (ret){
            packet.cancel();
            return;
        }

        reader.cancel(packet);
    }

    //从队列中取packet
    @Override
    public SendPacket takePacket(){
        SendPacket packet = queue.poll();
            if (packet == null) {
                return null;
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

        synchronized (isSending){
            if(isSending.get() || isClosed.get())
                return;
        }

        if (reader.requestTakePacket()){
            try {
                boolean isSucceed = sender.postSendAsync();

                if (isSucceed){
                    isSending.set(true);
                }

            } catch (IOException e) {
                closeAndNotify();
                e.printStackTrace();
            }
        }

    }


    private void closeAndNotify() {

        CloseUtils.close(this);
    }


    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false,true)){
            //reader关闭
            reader.close();


            queue.clear();

            synchronized (isSending) {
                isSending.set(false);
            }
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return isClosed.get() ? null : reader.fillData();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
        synchronized (isSending){
            isSending.set(false);
        }
        requestSend();
    }

    @Override
    public void oncConsumeCompleted(IoArgs args) {

        //继续发送当前包

        synchronized (isSending){
            isSending.set(false);
        }

        requestSend();
    }

}
