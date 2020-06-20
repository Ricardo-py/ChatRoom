package com.richard.sample.client;



import com.richard.library.clink.core.Connector;
import com.richard.library.clink.core.Packet;
import com.richard.library.clink.core.ReceivePacket;
import com.richard.library.clink.utils.CloseUtils;
import com.richard.sample.client.bean.ServerInfo;
import com.richard.sample.foo.Foo;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;

public class TCPClient extends Connector {

    private final File cachePath;

    public TCPClient(SocketChannel socketChannel,File cachePath) throws IOException {
        setup(socketChannel);
        this.cachePath = cachePath;
    }

    public void exit() {
        CloseUtils.close(this);
    }


    @Override
    public void onChannelClosed(SocketChannel channel){
        super.onChannelClosed(channel);
        System.out.println("连接已经关闭");
    }

    @Override
    protected File createNewReceiveFile() {

        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivePacket(ReceivePacket packet) {
        super.onReceivePacket(packet);
        if (packet.type() == Packet.TYPE_MEMORY_STRING){
            String string = (String) packet.entity();
            System.out.println(key.toString() + ":" + string);
        }
    }


    public static TCPClient startWith(ServerInfo info,File cachePath) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();

        // 连接本地，端口2000；超时时间3000ms
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketChannel.getRemoteAddress());

        try {
            return new TCPClient(socketChannel,cachePath);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }

        return null;
    }


    static class ReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();
            try {
                // 得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    String str;
                    try {
                        // 客户端拿到一条数据
                        str = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    if (str == null) {
                        System.out.println("连接已关闭，无法读取数据！");
                        break;
                    }
                    // 打印到屏幕
                    System.out.println(str);
                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开：" + e.getMessage());
                }
            } finally {
                // 连接关闭
                CloseUtils.close(inputStream);
            }
        }

        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }
}
