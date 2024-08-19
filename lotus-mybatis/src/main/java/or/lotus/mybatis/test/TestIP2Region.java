package or.lotus.mybatis.test;

import or.lotus.core.ip2reg.IP2Region;
import or.lotus.core.ip2reg.IpInfo;

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
