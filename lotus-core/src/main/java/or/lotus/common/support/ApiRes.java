package or.lotus.common.support;


public class ApiRes {
    public static final int C_SUCCESS         =   200;//成功
    public static final int C_FAIL            =   300;//操作失败
    public static final int C_ERROR           =   500;//未知错误
    public static final int C_AUTH_NOT_FOUND  =   401;//无token
    public static final int C_AUTH_FAIL       =   402;//token所属ip错误
    public static final int C_AUTH_PERMISSION_FAIL =   404;//权限不足


    public int code;
    public Object data = null;

    private ApiRes() {
    }

    public static ApiRes error() {
        return error(null);
    }

    public static ApiRes error(Object data) {
        return create(C_ERROR, data);
    }

    public static ApiRes fail() {
        return fail(null);
    }

    public static ApiRes fail(Object data) {
        return create(C_FAIL, data);
    }

    public static ApiRes success() {
        return success(null);
    }

    public static ApiRes success(Object data) {
        return create(C_SUCCESS, data);
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
