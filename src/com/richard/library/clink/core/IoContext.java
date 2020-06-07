package com.richard.library.clink.core;

import java.io.IOException;

public class IoContext {

    //单例的
    private static IoContext INSTANCE;

    //由于ioProvider是一个全局性的变量，所以将其写到IoContext上下文里面
    private final IoProvider ioProvider;

    private IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public static IoContext get() {
        return INSTANCE;
    }

    public static StartedBoot setup() {
        return new StartedBoot();
    }

    public static void close() throws IOException {
        if (INSTANCE != null) {
            INSTANCE.callClose();
        }
    }

    private void callClose() throws IOException {
        ioProvider.close();
    }


    //开始启动的类
    public static class StartedBoot {

        private IoProvider ioProvider;

        private StartedBoot() {

        }

        //传入ioProvider
        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }


        public IoContext start() {
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }
    }
}
