package or.lotus.obj;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class SessionInfo<T> {
    public T object;
    public String token;
    public String deviceType;
    public String deviceIp;
    public Long loginTime;

    public HashSet<String> permissions;

    public Object attr;
    public ArrayList<Long> errList;

    public SessionInfo(T object, String deviceType, String deviceIp) {
        this.object = object;
        this.deviceType = deviceType;
        this.deviceIp = deviceIp;

        loginTime = System.currentTimeMillis();
        errList = new ArrayList<>();
        this.permissions = new HashSet<>();
       /* String [] tmp = user.getPermissionsStr().split(",");
        for(String p : tmp) {
            this.permissions.add(p);
        }*/
    }

    public SessionInfo() { }

    public boolean isIpOk(String ip) {
        //暂时先不判断这个, 移动端会出现频繁的网络切换
        if(ip != null && ip.equals(deviceIp)) {
            return true;
        }
        return true;
    }

    /**
     * 当前登录账号是否有权限
     * @param need
     * @return
     */
    public synchronized boolean isInPermission(String[] need) {
        if(need != null && need.length > 0) {
            for(String np : need) {
                if(permissions.contains(np)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 当前登录账号是否有全部权限
     * @param need
     * @return
     */
    public synchronized boolean isFullPermission(String[] need) {
        if (need != null && need.length > 0 && permissions != null && need.length == permissions.size()) {
            for (String p : need) {
                if (!isInPermission(new String[]{p})) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 发生错误时, 调用此方法会记录下当前时间, 并根据time, maxErr判断是否在 time 时间内 错误 maxErr 次
     * @param time 时间范围 毫秒
     * @param maxErr 最大错误数量
     * @return 超出数量返回true
     */
    public boolean checkErrMaxCount(int time, int maxErr) {
        errList.add(System.currentTimeMillis());
        long start = System.currentTimeMillis() - time;
        List<Long> count = errList.stream().filter(t -> t > start).collect(Collectors.toList());

        if(count.size() >= maxErr) {
            errList.clear();
            return true;
        }
        return false;
    }

    public Object getAttr() {
        return attr;
    }

    public void setAttr(Object attr) {
        this.attr = attr;
    }



    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public HashSet<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(HashSet<String> permissions) {
        this.permissions = permissions;
    }

    public ArrayList<Long> getErrList() {
        return errList;
    }

    public void setErrList(ArrayList<Long> errList) {
        this.errList = errList;
    }
}
