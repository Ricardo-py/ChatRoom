package com.richard.library.clink.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 对byteBuffer进行一个封装，如果byteBuffer无节制地进行创建的话，内存
 * 会出现爆炸的情况，这个时候我们将其封装，并且里面有一些简化的操作和便捷的流程
 */
public class IoArgs {
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException {
        return channel.write(buffer);
    }

    public String bufferString() {
        // 丢弃换行符
        return new String(byteBuffer, 0, buffer.position() - 1);
    }

    //监听IoArgs的状态
    public interface IoArgsEventListener {

        //在其开始的时候有一个回调
        void onStarted(IoArgs args);

        //在其完成的时候有一个回调
        void onCompleted(IoArgs args);
    }
}
