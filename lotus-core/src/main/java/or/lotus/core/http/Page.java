package or.lotus.core.http;


import java.util.List;

public class Page<T> {
    public int page;
    public int total;
    public int totalPage;
    public List<T> list;

    public Page(int page, int total, int pageSize, List<T> list) {
        this.page = page;
        this.total = total;
        this.list = list;
        this.totalPage = (int) Math.ceil((double) total / pageSize);
    }

    public static int pageToStart(int page, int size) {
        return (page - 1) * size;
    }
}
