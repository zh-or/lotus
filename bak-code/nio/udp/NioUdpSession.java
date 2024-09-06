package or.lotus.core.nio.udp;

import java.net.SocketAddress;

import or.lotus.core.nio.NioContext;
import or.lotus.core.nio.Session;

public class NioUdpSession extends Session {

    public NioUdpSession(NioContext context, long id) {
        super(context, id);
    }

    @Override
    public int getWriteMessageSize() {
        return 0;
    }

    @Override
    public SocketAddress getLocaAddress() {
        return null;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public void write(Object data) {

    }

    @Override
    public void closeOnFlush() {

    }

}
