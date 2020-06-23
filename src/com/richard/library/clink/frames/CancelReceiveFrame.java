package com.richard.library.clink.frames;


import com.richard.library.clink.core.IoArgs;

import java.io.IOException;

/**
 * 取消传输帧，接收实现
 */
public class CancelReceiveFrame extends AbsReceiveFrame {

    CancelReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
