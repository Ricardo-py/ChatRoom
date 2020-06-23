package com.richard.Test.byteTest;

import java.util.Arrays;

public class Test {
    public static void main(String[] args){
        int number = 123143;
        System.out.println(Integer.toBinaryString(number));
        byte[] n1 = new byte[1];
        n1[0] = (byte)(number >> 8);
        byte n2 = (byte)number;

        System.out.println(byteToBit(n1[0]));
    }

    public static String byteToBit(byte b) {
        return ""
                + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
                + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
                + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                + (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1);
    }
}
