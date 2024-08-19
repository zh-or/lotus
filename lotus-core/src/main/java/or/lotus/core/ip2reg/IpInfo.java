package or.lotus.core.ip2reg;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public class IpInfo {

    /**
     * 国家
     */
    private String country;

    /**
     * 区域
     */
    private String region;

    /**
     * 省
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 运营商
     */
    private String isp;


    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\|");

    public IpInfo(String addressInfo) {

        String[] splitInfos = SPLIT_PATTERN.split(addressInfo);
        // 补齐5位
        if (splitInfos.length < 5) {
            splitInfos = Arrays.copyOf(splitInfos, 5);
        }
        country = filterZero(splitInfos[0]);
        region = filterZero(splitInfos[1]);
        province = filterZero(splitInfos[2]);
        city = filterZero(splitInfos[3]);
        isp = filterZero(splitInfos[4]);
    }

    private static String filterZero(String info) {
        // null 或 0 返回 null
        if (info == null || BigDecimal.ZERO.toString().equals(info)) {
            return null;
        }
        return info;
    }

    public String getAddress() {
        return getAddress("-");
    }

    /**
     * 拼接完整的地址
     * @return address
     */
    public String getAddress(String delimiter) {
        ArrayList<String> addr = new ArrayList<>(5);
        addr.add(country);
        addr.add(region);
        addr.add(province);
        addr.add(city);
        addr.removeIf(Objects::isNull);
        return String.join(delimiter, addr);
    }

    public String getAddressAndIsp() {
        return getAddressAndIsp("-");
    }

    /**
     * 拼接完整的地址
     * @return address
     */
    public String getAddressAndIsp(String delimiter) {
        ArrayList<String> addr = new ArrayList<>(5);
        addr.add(country);
        addr.add(region);
        addr.add(province);
        addr.add(city);
        addr.add(isp);
        addr.removeIf(Objects::isNull);
        return String.join(delimiter, addr);
    }

    @Override
    public String toString() {
        return getAddressAndIsp();
    }

    public String getCountry() {
        return country;
    }

    public String getRegion() {
        return region;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getIsp() {
        return isp;
    }
}
