package lotus.http.client;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLSocketFactory;

public class HttpClientSimple {
	private Socket socket;
	private URI uri;
	private boolean isConnection = false;

	private HttpClientSimple(URI uri) {
		this.uri = uri;
	}

	public static HttpClientSimple createByUrl(String url)
			throws URISyntaxException {
		URI uri = new URI(url);
		HttpClientSimple client = new HttpClientSimple(uri);
		return client;
	}

	public void sendRequest(Request request) {

	}

	public void connection(int timeout) throws Exception {
		if (isConnection) {
			return;
		}
		String scheme = uri.getScheme();
		switch (scheme) {
			case "https":
			    socket = SSLSocketFactory.getDefault().createSocket();
			    break;
    		case "http":
    		    socket = new Socket();
    			break;
    		default:
    		    throw new Exception("不支持的协议, 当前只支持 http/https");

		}
		
		socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), timeout);
	}
}
