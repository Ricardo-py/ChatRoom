package com.richard.library.clink.box;

import com.richard.library.clink.core.SendPacket;

import java.io.*;

public class FileSendPacket extends SendPacket<FileInputStream> {

    public FileSendPacket(File file) {
        this.length = file.length();
    }


    @Override
    protected FileInputStream createStream() {
        return null;
    }

}
