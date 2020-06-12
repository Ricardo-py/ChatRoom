package com.richard.library.clink.impl.async;

import com.richard.library.clink.core.IoArgs;
import com.richard.library.clink.core.SendDispatcher;
import com.richard.library.clink.core.SendPacket;
import com.richard.library.clink.core.Sender;
import com.richard.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {

    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);


    private IoArgs ioArgs = new IoArgs();
    private SendPacket packetTemp;

    private int total;
    private int position;

    public AsyncSendDispatcher(Sender sender){
        this.sender = sender;
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

        IoArgs args = ioArgs;

        //开始，清理
        args.startWriting();

        if (position >= total){
            sendNextPacket();
            return;
        }else if(position == 0){
            //首包，需要携带长度信息
            args.writeLength(total);
        }
        byte[] bytes = packetTemp.bytes();

        //把bytes的数据写入到IoArgs
        int count = args.readFrom(bytes,position);

        position += count;

        //完成封装
        args.finishWriting();

        try {
            sender.sendAsync(args,ioArgsEventListener);
        } catch (IOException e) {
            closeAndNotify();
            e.printStackTrace();
        }

    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {

            //继续发送当前包
            //System.out.println("继续发包");
            sendCurrentPacket();
        }
    };

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false,true)){
            isSending.set(false);
            SendPacket packet = this.packetTemp;
            if (packet != null){
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }
}
