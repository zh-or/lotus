package lotus.http.server.support;

import java.nio.Buffer;

public class FrameBuffer {
    private WebSocketFrame frame;
    
    public FrameBuffer(WebSocketFrame frame) {
        this.frame = frame;
    }

    
    /**
     * 封包
     * @param buffer
     */
    public void wrap(Buffer buffer) {
        
    }
    
    /**
     * 解包
     * @param buffer
     * @return
     */
    public boolean unWrap(Buffer buffer) {
        
        return false;
    }
}
