package com.richard.sample.client;



import com.richard.library.clink.box.FileSendPacket;
import com.richard.library.clink.core.IoContext;
import com.richard.library.clink.impl.IoSelectorProvider;
import com.richard.sample.client.bean.ServerInfo;
import com.richard.sample.foo.Foo;

import java.io.*;

public class Client {
    public static void main(String[] args) throws IOException {

        File cachePath = Foo.getCacheDir("client");

        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Client:" + info);

        if (info != null) {
            TCPClient tcpClient = null;

            try {
                tcpClient = TCPClient.startWith(info,cachePath);
                if (tcpClient == null) {
                    return;
                }
                write(tcpClient);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }

        IoContext.close();
    }


    private static void write(TCPClient tcpClient) throws IOException, InterruptedException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            if (str == null || str.length() == 0
                    || "00bye00".equalsIgnoreCase(str)) {
                break;
            }
            if (str.startsWith("--f")){
                String[] array = str.split(" ");
                if (array.length >= 2){
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()){
                        FileSendPacket packet = new FileSendPacket(file);
                        tcpClient.send(packet);
                        continue;
                    }
                }
            }

            //System.out.println(str.length());
                // 发送字符串
                tcpClient.send(str);
//                tcpClient.send(str);
//                //Thread.sleep(1000);
//                tcpClient.send(str);
//                //Thread.sleep(1000);
//                tcpClient.send(str);


        } while (true);
    }

}
