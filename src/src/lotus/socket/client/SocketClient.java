package lotus.socket.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import lotus.socket.common.ClientCallback;

public class SocketClient {
    
    /* 待重写*/
    
	private Socket              socket                   = null;
    private ClientCallback      callback                 = null;
    private ExecutorService     sendpool                 = null;
    private ExecutorService     eventpool                = null;
    private OutputStream        out                      = null;
    private InputStream         in                       = null;
    private long                lasthtime                = 0;
    private int                 keepalive                = 180 * 1000;
    private byte[]              keepcontent              = {};
    private Object              attr                     = null;//辅助参数
    private String              host                     = "0.0.0.0";
    private int                 port                     = 5000;
    private int                 timeout                  = 10000;
    private int                 RECV_BUFF_SIZE           = 1024 * 2;
    
	
	
}
