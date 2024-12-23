package or.lotus.core.swl;

import java.util.Arrays;

public class SlideWindowObj {
    long lastTime = 0;
    int limitSec;
    int[] windows;


    public SlideWindowObj() {
    }

    public SlideWindowObj(int windowLimit, int limitSec) {
        this.windows = new int[windowLimit];
        this.limitSec = limitSec;
    }

    public synchronized boolean check(int count) {
        long now = System.currentTimeMillis();
        int move = (int) ((now - lastTime) / 1000 / limitSec);

        int wLen = windows.length;
        if(move < 0) {
            move = 0;
        }
        if(move >= wLen) {
            for(int i = 0; i < wLen; i++) {
                windows[i] = 0;
            }
            lastTime = now;
        } else if(move > 0) {
            System.arraycopy(windows, move, windows, 0, wLen - move);
            for(int i = wLen - move; i < wLen; i++) {
                windows[i] = 0;
            }
            lastTime = now;
        }

        windows[wLen - 1] ++;
        int sum = 0;
        for(int i = 0; i < wLen; i++) {
            sum += windows[i];
            if(sum > count) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "SlideWindowObj{" +
                "lastTime=" + lastTime +
                ", limitSec=" + limitSec +
                ", windows=" + Arrays.toString(windows) +
                '}';
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public int getLimitSec() {
        return limitSec;
    }

    public void setLimitSec(int limitSec) {
        this.limitSec = limitSec;
    }

    public int[] getWindows() {
        return windows;
    }

    public void setWindows(int[] windows) {
        this.windows = windows;
    }

}
