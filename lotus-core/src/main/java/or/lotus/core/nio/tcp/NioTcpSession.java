package or.lotus.core.nio.tcp;


import or.lotus.core.nio.support.ProtocolCodec;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;


public class NioTcpSession  {
	protected ConcurrentHashMap<Object, Object> attrs;
	protected long lastActive;
	protected ProtocolCodec codec = null;
	private SocketChannel channel;
	private SelectionKey key;

	public NioTcpSession(SocketChannel channel) {
		this.channel = channel;
		this.attrs = new ConcurrentHashMap<Object, Object>();
	}

	public void setKey(SelectionKey key){
	    this.key = key;
	}

	public SocketAddress getRemoteAddress() {
		try {
            return channel.getRemoteAddress();
        } catch (IOException e) {
        }
		return null;
	}

	public SocketAddress getLocalAddress() {
	    try {
            return channel.getLocalAddress();
        } catch (IOException e) {}
	    return null;
	}

	public synchronized void setLastActive(long t) {
		lastActive = t;
	}

	public long getLastActive() {
		return lastActive;
	}

	public Object getAttr(Object key, Object def) {
		Object val = attrs.get(key);
		if(val == null) return def;
		return val;
	}

	public Object getAttr(Object key) {
		return getAttr(key, null);
	}

	public void setAttr(Object key, Object val) {
		attrs.put(key, val);
	}

	public Object removeAttr(Object key){
		return attrs.remove(key);
	}

	public ProtocolCodec getCodec() {
		return codec;
	}

	public void setCodec(ProtocolCodec codec) {
		this.codec = codec;
	}

}
