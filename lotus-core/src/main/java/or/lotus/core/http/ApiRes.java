package or.lotus.core.http;


import com.fasterxml.jackson.core.JsonProcessingException;
import or.lotus.core.common.BeanUtils;

public class ApiRes {
    public static int SUCCESS         =   200;//成功
    public static int FAIL            =   300;//操作失败
    public static int ERROR           =   500;//未知错误
    public static int AUTH_NOT_FOUND  =   401;//无token
    public static int AUTH_FAIL       =   402;//token所属ip错误
    public static int AUTH_PERMISSION_FAIL =   403;//权限不足


    public int code;
    public Object data = null;

    private ApiRes() {
    }

    @Override
    public String toString() {
        try {
            return BeanUtils.ObjToJson(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ApiRes error() {
        return error(null);
    }

    public static ApiRes error(Object data) {
        return create(ERROR, data);
    }

    public static ApiRes fail() {
        return fail(null);
    }

    public static ApiRes fail(Object data) {
        return create(FAIL, data);
    }

    public static ApiRes success() {
        return success(null);
    }

    public static ApiRes success(Object data) {
        return create(SUCCESS, data);
    }

    /**
     *
     * @param code 200 成功, 300 需要前端处理的错误, 500 错误
     * @return
     */
    public static ApiRes create(int code) {
        ApiRes apiRes = new ApiRes();
        apiRes.code = code;
        return apiRes;
    }

    /**
     *
     * @param code 200 成功, 300 需要前端处理的错误, 500 错误
     * @param data
     * @return
     */
    public static ApiRes create(int code, Object data) {
        ApiRes apiRes = new ApiRes();
        apiRes.code = code;
        apiRes.data = data;
        return apiRes;
    }

}
