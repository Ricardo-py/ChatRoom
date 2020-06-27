package com.richard.library.clink.impl.async;

import com.richard.library.clink.box.StringReceivePacket;
import com.richard.library.clink.core.*;
import com.richard.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher, IoArgs.IoArgsEventProcessor, AsyncPacketWriter.PacketProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;

    private final ReceivePacketCallback callback;

    private final AsyncPacketWriter writer = new AsyncPacketWriter(this);

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {

        //这里的receiver是socketChannelAdaptor的具体实现类
        this.receiver = receiver;

        this.receiver.setReceiveListener(this);

        //这里的callback是Connector中的receivePacketCallback
        this.callback = callback;
    }

    @Override
    public void start() {
        registerReceive();
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
            e.printStackTrace();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false,true)){
            writer.close();
        }
    }



    @Override
    public IoArgs provideIoArgs() {

        return writer.takeIoArgs();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void oncConsumeCompleted(IoArgs args) {

        if (isClosed.get())
            return;

        do {
            writer.consumeIoArgs(args);
        }while(args.remained() && !isClosed.get());

        registerReceive();
    }

    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {

        return callback.onArrivedNewPacket(type,length);
    }

    @Override
    public void completedPacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }
}
