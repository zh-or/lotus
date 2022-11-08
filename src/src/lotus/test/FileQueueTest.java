package lotus.test;

import java.io.IOException;

import lotus.queue.FileQueue;
import lotus.utils.Utils;

public class FileQueueTest {
    
    public static void main(String[] args) throws IOException {
        FileQueue fq = new FileQueue("./urls.txt");
        
        new Thread(() -> {
            int i = 0;
            do {
                try {
                    fq.push("http://123awkdkliopdwaw.caowd" + Utils.RandomNum(0, 1000) + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i ++;
                Utils.SLEEP(10);
            } while(i < 1000);
            
        }).start();  
        new Thread(() -> {
            int i = 0;
            do {
                try {
                    fq.push("http://123awkdkliopdwaw.caowd" + Utils.RandomNum(0, 1000) + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i ++;
                Utils.SLEEP(10);
            } while(i < 1000);
            
        }).start();
        
        do {
            String line = fq.pollAndWait(0);
            System.out.println("read:" + line);
        } while(true);
    }
}
