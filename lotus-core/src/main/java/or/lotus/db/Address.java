package or.lotus.db;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;

public class Address {
    public String name;
    public String displayName;
    public String mac;
    public List<InterfaceAddress> ips;

    public NetworkInterface net;

    public Address(NetworkInterface net) throws SocketException {
        this(
                net.getName(),
                net.getDisplayName(),
                net.getHardwareAddress(),
                net.getInterfaceAddresses()
        );
        this.net = net;
    }

    public Address(String name, String displayName, byte[] macBytes, List<InterfaceAddress> ips) {
        this.name = name;
        this.displayName = displayName;
        if(macBytes != null) {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < macBytes.length; i++) {
                sb.append(String.format("%02X", macBytes[i]));
                if(i != macBytes.length - 1) {
                    sb.append(":");
                }
            }
            this.mac = sb.toString();
        } else {
            this.mac = "";
        }


        this.ips = ips;
    }

    @Override
    public String toString() {
        return "Address{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", mac='" + mac + '\'' +
                ", ips=" + ips +
                '}';
    }
}
