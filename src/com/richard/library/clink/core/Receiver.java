package com.richard.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    boolean receiveAsync(IoArgs args) throws IOException;

    boolean setReceiveListener(IoArgs.IoArgsEventListener listener);

}
