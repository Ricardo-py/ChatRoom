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

public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor {

    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);


    private IoArgs ioArgs = new IoArgs();
    private SendPacket<?> packetTemp;

    private ReadableByteChannel packetChannel;

    private long total;
    private long position;

    public AsyncSendDispatcher(Sender sender){
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        //将消息加到队列当中
        queue.offer(packet);

        //如果没有其他线程在发送消息我们就对消息进行发送
        if (isSending.compareAndSet(false,true)){
            sendNextPacket();
        }
    }

    @Override
    public void cancel(SendPacket packet) {

    }

    //从队列中取packet
    private SendPacket takePacket(){
        SendPacket packet = queue.poll();

        //如果消息失效的话，继续取消息
        if (packet != null && packet.isCanceled()){
            return takePacket();
        }

        return packet;
    }

    private void sendNextPacket(){
        SendPacket temp = packetTemp;

        if (temp != null){
            CloseUtils.close(temp);
        }

        //从队列当中取一条有效的消息
        SendPacket packet = packetTemp = takePacket();

        //如果没有消息的话，将发送消息的状态设置为false，并且返回
        if (packet == null){
            isSending.set(false);
            return ;
        }

        //如果有消息的话，设置total为消息的长度
        total = packet.length();

        //设置Position为0
        position = 0;

        //然后发消息
        sendCurrentPacket();
    }

    private void sendCurrentPacket() {

        if (position >= total){
            completePacket(position == total);
            sendNextPacket();
            return;
        }

        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
            e.printStackTrace();
        }

    }

    /**
     * packet是否发送成功
     * @param isSucceed
     */
    private void completePacket(boolean isSucceed){
        SendPacket packet = packetTemp;
        if (packet == null){
            return;
        }
        CloseUtils.close(packet);
        CloseUtils.close(packetChannel);

        packetTemp = null;
        packetChannel = null;
        total = 0;
        position = 9;
    }


    private void closeAndNotify() {
        CloseUtils.close(this);
    }


    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false,true)){
            isSending.set(false);
            //异常关闭导致的完成操作
            completePacket(false);
        }
    }

    @Override
    public IoArgs provideIoArgs() {

        IoArgs args = ioArgs;

        if (packetChannel == null){
            packetChannel = Channels.newChannel(packetTemp.open());

            args.limit(4);

            //这里需要注意(int)的强转问题
            args.writeLength((int) packetTemp.length());

        }else{
            //这里可以强转
            args.limit((int) Math.min(args.capacity(),total - position));
            try {
                int count = args.readFrom(packetChannel);
                position += count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void oncConsumeCompleted(IoArgs args) {
        sendCurrentPacket();
    }
}
