package or.lotus.files;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class FilesTest {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(9527));
        Socket socket = server.accept();

        socket.getOutputStream();

    }
}
