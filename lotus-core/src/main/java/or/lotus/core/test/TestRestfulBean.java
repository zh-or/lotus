package or.lotus.core.test;


import or.lotus.core.http.restful.ann.Bean;

public class TestRestfulBean {

    @Bean(value = "b1", order = 1)
    public String testBean() {
        return "testBean1";
    }


    @Bean(value = "b2", order = 2)
    public String testBean1() {
        return "testBean2";
    }
}
