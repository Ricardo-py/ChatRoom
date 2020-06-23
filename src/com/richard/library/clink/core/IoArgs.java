package com.richard.library.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.WriteAbortedException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * 对byteBuffer进行一个封装，如果byteBuffer无节制地进行创建的话，内存
 * 会出现爆炸的情况，这个时候我们将其封装，并且里面有一些简化的操作和便捷的流程
 */
public class IoArgs {

    private final static int initial_limit = 256;

    private int limit = 256;
    private ByteBuffer buffer = ByteBuffer.allocate(limit);


    /**
     * 从bytes数组进行消费
     * @param bytes
     * @param offset
     * @param count
     * @return
     */
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count,buffer.remaining());
        if (size <= 0)
            return 0;
        buffer.put(bytes,offset,size);
        return size;
    }

    public int writeTo(byte[] bytes,int offset){
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes,offset,size);
        return size;
    }

    /**
     *
     * @param channel
     * @return
     * @throws IOException
     */
    public int readFrom(ReadableByteChannel channel) throws IOException {
        //startWriting();
        int bytesProduced = 0;
        if(buffer.hasRemaining()){
            //System.out.println(flag);
            int len = channel.read(buffer);
            //System.out.println(buffer);
            //System.out.println(buffer);
            if (len < 0) {
                //System.out.println("len小于0了");
                //break;
                throw new EOFException();
            }
            bytesProduced += len;
        }

        //finishWriting();
        return bytesProduced;
    }


    /**
     *
     * @param channel
     * @return
     * @throws IOException
     */
    public int writeTo(WritableByteChannel channel) throws IOException {
        //startWriting();
        int bytesProduced = 0;

        if(buffer.hasRemaining()){
            int len = channel.write(buffer);
            if (len < 0)
                throw new EOFException();
            bytesProduced += len;
        }
        //finishWriting();
        return bytesProduced;
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

        if(buffer.hasRemaining()){
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
        //startWriting();
        int bytesProduced = 0;

        if(buffer.hasRemaining()){
            int len = channel.write(buffer);
            if (len < 0)
                throw new EOFException();
            bytesProduced += len;
        }
        //finishWriting();
        return bytesProduced;
    }

    /**
     * 开始写数据到IoArgs
     */
    public void startWriting(){
        buffer.clear();
        buffer.limit(limit);
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
        this.limit = Math.min(limit,buffer.capacity());
        this.buffer.limit(this.limit);
    }

//    public void writeLength(int total) {
//        buffer.clear();
//        this.limit = initial_limit;
//        this.buffer.limit(initial_limit);
//        buffer.putInt(total);
//        finishWriting();
//    }

    public int readLength(){
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean remained() {
        return buffer.remaining() > 0;
    }

    public int fillEmpty(int size) {
        int fillSize = Math.min(size,buffer.remaining());
        buffer.position(buffer.position() + fillSize);
        return fillSize;
    }

    /**
     * 清空部分数据
     *
     * @param size 想要清空的数据长度
     * @return 真实清空的数据长度
     */
    public int setEmpty(int size) {
        int emptySize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + emptySize);
        return emptySize;
    }


    /**
     * IoArgs 提供者，处理者；数据的生产者或消费者
     */
    public interface IoArgsEventProcessor {

        /**
         * 提供一份可消费的IoArgs
         * @return
         */
        IoArgs provideIoArgs();


        /**
         * 消费失败时回调
         * @param args
         * @param e
         */
        void onConsumeFailed(IoArgs args,Exception e);

        /**
         *消费成功
         * @param args
         */
        void oncConsumeCompleted(IoArgs args);

    }


}
