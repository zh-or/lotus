package lotus.test;

import lotus.utils.NamedClass;
import lotus.utils.NamedClass.NamedAnnotation;

@NamedAnnotation(name = "class")
public class Test{
    public String a;
    public int d;
    public long l;
    public boolean b;
    
    
    
    public Test() {}


    @Override
    public String toString() {
        return "Test [a=" + a + ", d=" + d + ", l=" + l + ", b=" + b + "]";
    }

    
    @NamedAnnotation(name ="/test")
    public int testM(int arg) {
        System.out.println("call test");
        return arg;
    }
    
    public static void main(String[] args) throws Exception {
        Test test = new Test();
        NamedClass nc = new NamedClass();
        nc.registerClass(test);
        int r = (int) nc.callMenthodByName("class/test", 123);
        System.out.println("调用返回:" + r);
    }
}