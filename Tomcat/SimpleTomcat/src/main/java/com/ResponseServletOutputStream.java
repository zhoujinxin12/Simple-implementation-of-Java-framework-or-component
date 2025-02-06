package com;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;

public class ResponseServletOutputStream extends ServletOutputStream {

    private byte[] bytes = new byte[1024];
    private int pos = 0;

    private void expandCapacity() {
        byte[] newBytes = new byte[bytes.length * 2];
        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        bytes = newBytes;
    }
    @Override
    public void write(int b) throws IOException {
        if (pos >= bytes.length) {
            expandCapacity();
        }
        bytes[pos++] = (byte) b;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getPos() {
        return pos;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {

    }
}
