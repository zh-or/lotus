package or.lotus.test;

import or.lotus.common.support.Address;
import or.lotus.common.support.Utils;

import java.util.ArrayList;

public class TestCommon {
    public static void main(String[] args) {
        ArrayList<Address> address = Utils.getNetworkInfo(true);

        System.out.println(address.toString());
    }
}
