package com.richard.sample.server.handle;



import com.richard.library.clink.core.Connector;
import com.richard.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler extends Connector {
    //private final SocketChannel socketChannel;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;
    private SocketChannel channel;

    //当有客户端连接的时候，就进入到这个构造函数
    //进入这个构造函数
    //这里的回调传入的是TCPServer

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {


       // this.socketChannel = socketChannel;
        this.clientHandlerCallback = clientHandlerCallback;

        this.clientInfo = socketChannel.getRemoteAddress().toString();

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


    private void exitBySelf() {
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }


    @Override
    protected void onReceiveNewMessage(String str){
        super.onReceiveNewMessage(str);
        clientHandlerCallback.onNewMessageArrived(this,str);
    }

    public interface ClientHandlerCallback {
        // 自身关闭通知
        void onSelfClosed(ClientHandler handler);

        // 收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);
    }

}
