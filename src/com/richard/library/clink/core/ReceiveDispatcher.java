package com.richard.library.clink.core;


import java.io.Closeable;

/**
 * 接受的数据调度封装
 * 把一份或多分IoArgs组合成一份Packet
 */
public interface ReceiveDispatcher extends Closeable {

    void start();

    void stop();

    interface ReceivePacketCallback{
        void onReceivePacketCompleted(ReceivePacket packet);
    }
}
