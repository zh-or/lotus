package or.lotus.core.http.server;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

public class TemplateWriter extends Writer {
    private ByteBuf buffer;
    private Charset charset;

    protected TemplateWriter(ByteBuf buffer, Charset charset) {
        this.buffer = buffer;
        this.charset = charset;
    }

    public ByteBuf getBuffer() {
        return buffer;
    }

    @Override
    public void write(int c) throws IOException {
        buffer.writeByte(c);
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        for(char c : cbuf) {
            buffer.writeByte(c);
        }
    }

    @Override
    public void write(String str) throws IOException {
        buffer.writeCharSequence(str, charset);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        buffer.writeCharSequence(str.subSequence(off, off + len), charset);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        int max = off + len;
        for(int i = off; i < max; i ++ ) {
            buffer.writeByte(cbuf[i]);
        }
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
