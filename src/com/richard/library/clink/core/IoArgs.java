package com.richard.library.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 对byteBuffer进行一个封装，如果byteBuffer无节制地进行创建的话，内存
 * 会出现爆炸的情况，这个时候我们将其封装，并且里面有一些简化的操作和便捷的流程
 */
public class IoArgs {

    private int limit = 5;

    private byte[] byteBuffer = new byte[5];

    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    /**
     * 从bytes中读取数据
     * @param bytes
     * @param offset
     * @return
     */
    public int readFrom(byte[] bytes,int offset){
        int size = Math.min(bytes.length - offset,buffer.remaining());

        buffer.put(bytes,offset,size);
        return size;
    }

    /**
     * 写入数据到bytes中
     * @param bytes
     * @param offset
     * @return
     */
    public int writeTo(byte[] bytes, int offset, int length){
        int size = Math.min(length - offset,buffer.remaining());
        buffer.get(bytes,offset,size);
        return size;
    }

    /**
     * 从socketChannel中读取数据
     * @param channel
     * @return
     * @throws IOException
     */
    public int readFrom(SocketChannel channel) throws IOException {
        startWriting();

        int bytesProduced = 0;

        while(buffer.hasRemaining()){
            //System.out.println(flag);
            int len = channel.read(buffer);
            //System.out.println(buffer);
            if (len < 0)
                throw new EOFException();
            bytesProduced += len;
        }
        //System.out.println(bytesProduced);

        finishWriting();
        return bytesProduced;
    }

    /**
     * 写数据到socketChannel
     * @param channel
     * @return
     * @throws IOException
     */
    public int writeTo(SocketChannel channel) throws IOException {

        startWriting();

        int bytesProduced = 0;

        while(buffer.hasRemaining()){
            int len = channel.write(buffer);
            if (len < 0)
                throw new EOFException();
            bytesProduced += len;
        }
        finishWriting();
        return bytesProduced;
    }

    /**
     * 开始写数据到IoArgs
     */
    public void startWriting(){
        buffer.clear();
        buffer.limit();
    }

    /**
     * 写完数据后调用
     */
    public void finishWriting(){
        buffer.flip();
    }


    /**
     * 设置单次操作的容纳区间
     * @param limit
     */
    public void limit(int limit){
        this.limit = limit;
    }

    public void writeLength(int total) {
        buffer.putInt(total);
    }

    public int readLength(){
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

//    public String bufferString() {
//        // 丢弃换行符
//        //System.out.println("bytebuffer_position:"+buffer.position());
//        return new String(byteBuffer, 0, buffer.position() - 1);
//    }

    //监听IoArgs的状态
    public interface IoArgsEventListener {

        //在其开始的时候有一个回调
        void onStarted(IoArgs args);

        //在其完成的时候有一个回调
        void onCompleted(IoArgs args);
    }
}
