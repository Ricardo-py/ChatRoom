package com.richard.library.clink.box;

import com.richard.library.clink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class StringSendPacket extends SendPacket<ByteArrayInputStream> {

    private final byte[] bytes;


    public StringSendPacket(String msg){
        this.bytes = msg.getBytes();
        this.length = bytes.length;
    }


    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }

}
