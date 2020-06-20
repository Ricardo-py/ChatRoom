package com.richard.sample.server.handle;



import com.richard.library.clink.core.Connector;
import com.richard.library.clink.core.Packet;
import com.richard.library.clink.core.ReceivePacket;
import com.richard.library.clink.utils.CloseUtils;
import com.richard.sample.foo.Foo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler extends Connector {
    //private final SocketChannel socketChannel;
    private final File cachePath;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;
    private SocketChannel channel;

    //当有客户端连接的时候，就进入到这个构造函数
    //进入这个构造函数
    //这里的回调传入的是TCPServer

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback, File cachePath) throws IOException {


       // this.socketChannel = socketChannel;
        this.clientHandlerCallback = clientHandlerCallback;

        this.clientInfo = socketChannel.getRemoteAddress().toString();

        this.cachePath = cachePath;

        setup(socketChannel);

        System.out.println("新客户端连接：" + clientInfo);

    }


    public void exit() {

        CloseUtils.close(this);
        System.out.println("客户端已退出：" + clientInfo);
    }

    @Override
    public void onChannelClosed(SocketChannel channel){
        super.onChannelClosed(channel);
        exitBySelf();
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
            clientHandlerCallback.onNewMessageArrived(this,string);
        }
    }

    private void exitBySelf() {
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }




    public interface ClientHandlerCallback {
        // 自身关闭通知
        void onSelfClosed(ClientHandler handler);

        // 收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);
    }

}
