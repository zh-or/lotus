package or.lotus.test;

import or.lotus.obj.IpInfo;
import or.lotus.service.IP2Region;

public class TestIP2Region {

    public static void main(String[] args) throws Exception {
        try(IP2Region ip2Region = new IP2Region("./ip2region.xdb")) {
            long start = System.currentTimeMillis();
            IpInfo info = ip2Region.search("119.86.73.159");
            start = System.currentTimeMillis() - start;
            System.out.println("用时: " + start + "ms, ->" + info.getAddressAndIsp());
        }
    }
}
