package or.lotus.core.nio.http;


/**
 * Standard HTTP header names.
 * <p>
 * These are all defined as lowercase to support HTTP/2 requirements while also not
 * violating HTTP/1.x requirements.  New header names should always be lowercase.
 */
public final class HttpHeaderNames {
    /**
     * {@code "accept"}
     */
    public static final String ACCEPT = ("accept");
    /**
     * {@code "accept-charset"}
     */
    public static final String ACCEPT_CHARSET = ("accept-charset");
    /**
     * {@code "accept-encoding"}
     */
    public static final String ACCEPT_ENCODING = ("accept-encoding");
    /**
     * {@code "accept-language"}
     */
    public static final String ACCEPT_LANGUAGE = ("accept-language");
    /**
     * {@code "accept-ranges"}
     */
    public static final String ACCEPT_RANGES = ("accept-ranges");
    /**
     * {@code "accept-patch"}
     */
    public static final String ACCEPT_PATCH = ("accept-patch");
    /**
     * {@code "access-control-allow-credentials"}
     */
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS =
            ("access-control-allow-credentials");
    /**
     * {@code "access-control-allow-headers"}
     */
    public static final String ACCESS_CONTROL_ALLOW_HEADERS =
            ("access-control-allow-headers");
    /**
     * {@code "access-control-allow-methods"}
     */
    public static final String ACCESS_CONTROL_ALLOW_METHODS =
            ("access-control-allow-methods");
    /**
     * {@code "access-control-allow-origin"}
     */
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN =
            ("access-control-allow-origin");
    /**
     * {@code "access-control-allow-origin"}
     */
    public static final String ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK =
            ("access-control-allow-private-network");
    /**
     * {@code "access-control-expose-headers"}
     */
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS =
            ("access-control-expose-headers");
    /**
     * {@code "access-control-max-age"}
     */
    public static final String ACCESS_CONTROL_MAX_AGE = ("access-control-max-age");
    /**
     * {@code "access-control-request-headers"}
     */
    public static final String ACCESS_CONTROL_REQUEST_HEADERS =
            ("access-control-request-headers");
    /**
     * {@code "access-control-request-method"}
     */
    public static final String ACCESS_CONTROL_REQUEST_METHOD =
            ("access-control-request-method");
    /**
     * {@code "access-control-request-private-network"}
     */
    public static final String ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK =
            ("access-control-request-private-network");
    /**
     * {@code "age"}
     */
    public static final String AGE = ("age");
    /**
     * {@code "allow"}
     */
    public static final String ALLOW = ("allow");
    /**
     * {@code "authorization"}
     */
    public static final String AUTHORIZATION = ("authorization");
    /**
     * {@code "cache-control"}
     */
    public static final String CACHE_CONTROL = ("cache-control");
    /**
     * {@code "connection"}
     */
    public static final String CONNECTION = ("connection");
    /**
     * {@code "content-base"}
     */
    public static final String CONTENT_BASE = ("content-base");
    /**
     * {@code "content-encoding"}
     */
    public static final String CONTENT_ENCODING = ("content-encoding");
    /**
     * {@code "content-language"}
     */
    public static final String CONTENT_LANGUAGE = ("content-language");
    /**
     * {@code "content-length"}
     */
    public static final String CONTENT_LENGTH = ("content-length");
    /**
     * {@code "content-location"}
     */
    public static final String CONTENT_LOCATION = ("content-location");
    /**
     * {@code "content-transfer-encoding"}
     */
    public static final String CONTENT_TRANSFER_ENCODING = ("content-transfer-encoding");
    /**
     * {@code "content-disposition"}
     */
    public static final String CONTENT_DISPOSITION = ("content-disposition");
    /**
     * {@code "content-md5"}
     */
    public static final String CONTENT_MD5 = ("content-md5");
    /**
     * {@code "content-range"}
     */
    public static final String CONTENT_RANGE = ("content-range");
    /**
     * {@code "content-security-policy"}
     */
    public static final String CONTENT_SECURITY_POLICY = ("content-security-policy");
    /**
     * {@code "content-type"}
     */
    public static final String CONTENT_TYPE = ("content-type");
    /**
     * {@code "cookie"}
     */
    public static final String COOKIE = ("cookie");
    /**
     * {@code "date"}
     */
    public static final String DATE = ("date");
    /**
     * {@code "dnt"}
     */
    public static final String DNT = ("dnt");
    /**
     * {@code "etag"}
     */
    public static final String ETAG = ("etag");
    /**
     * {@code "expect"}
     */
    public static final String EXPECT = ("expect");
    /**
     * {@code "expires"}
     */
    public static final String EXPIRES = ("expires");
    /**
     * {@code "from"}
     */
    public static final String FROM = ("from");
    /**
     * {@code "host"}
     */
    public static final String HOST = ("host");
    /**
     * {@code "if-match"}
     */
    public static final String IF_MATCH = ("if-match");
    /**
     * {@code "if-modified-since"}
     */
    public static final String IF_MODIFIED_SINCE = ("if-modified-since");
    /**
     * {@code "if-none-match"}
     */
    public static final String IF_NONE_MATCH = ("if-none-match");
    /**
     * {@code "if-range"}
     */
    public static final String IF_RANGE = ("if-range");
    /**
     * {@code "if-unmodified-since"}
     */
    public static final String IF_UNMODIFIED_SINCE = ("if-unmodified-since");
    /**
     * @deprecated use {@link #CONNECTION}
     *
     * {@code "keep-alive"}
     */
    @Deprecated
    public static final String KEEP_ALIVE = ("keep-alive");
    /**
     * {@code "last-modified"}
     */
    public static final String LAST_MODIFIED = ("last-modified");
    /**
     * {@code "location"}
     */
    public static final String LOCATION = ("location");
    /**
     * {@code "max-forwards"}
     */
    public static final String MAX_FORWARDS = ("max-forwards");
    /**
     * {@code "origin"}
     */
    public static final String ORIGIN = ("origin");
    /**
     * {@code "pragma"}
     */
    public static final String PRAGMA = ("pragma");
    /**
     * {@code "proxy-authenticate"}
     */
    public static final String PROXY_AUTHENTICATE = ("proxy-authenticate");
    /**
     * {@code "proxy-authorization"}
     */
    public static final String PROXY_AUTHORIZATION = ("proxy-authorization");
    /**
     * @deprecated use {@link #CONNECTION}
     *
     * {@code "proxy-connection"}
     */
    @Deprecated
    public static final String PROXY_CONNECTION = ("proxy-connection");
    /**
     * {@code "range"}
     */
    public static final String RANGE = ("range");
    /**
     * {@code "referer"}
     */
    public static final String REFERER = ("referer");
    /**
     * {@code "retry-after"}
     */
    public static final String RETRY_AFTER = ("retry-after");
    /**
     * {@code "sec-websocket-key1"}
     */
    public static final String SEC_WEBSOCKET_KEY1 = ("sec-websocket-key1");
    /**
     * {@code "sec-websocket-key2"}
     */
    public static final String SEC_WEBSOCKET_KEY2 = ("sec-websocket-key2");
    /**
     * {@code "sec-websocket-location"}
     */
    public static final String SEC_WEBSOCKET_LOCATION = ("sec-websocket-location");
    /**
     * {@code "sec-websocket-origin"}
     */
    public static final String SEC_WEBSOCKET_ORIGIN = ("sec-websocket-origin");
    /**
     * {@code "sec-websocket-protocol"}
     */
    public static final String SEC_WEBSOCKET_PROTOCOL = ("sec-websocket-protocol");
    /**
     * {@code "sec-websocket-version"}
     */
    public static final String SEC_WEBSOCKET_VERSION = ("sec-websocket-version");
    /**
     * {@code "sec-websocket-key"}
     */
    public static final String SEC_WEBSOCKET_KEY = ("sec-websocket-key");
    /**
     * {@code "sec-websocket-accept"}
     */
    public static final String SEC_WEBSOCKET_ACCEPT = ("sec-websocket-accept");
    /**
     * {@code "sec-websocket-protocol"}
     */
    public static final String SEC_WEBSOCKET_EXTENSIONS = ("sec-websocket-extensions");
    /**
     * {@code "server"}
     */
    public static final String SERVER = ("server");
    /**
     * {@code "set-cookie"}
     */
    public static final String SET_COOKIE = ("set-cookie");
    /**
     * {@code "set-cookie2"}
     */
    public static final String SET_COOKIE2 = ("set-cookie2");
    /**
     * {@code "te"}
     */
    public static final String TE = ("te");
    /**
     * {@code "trailer"}
     */
    public static final String TRAILER = ("trailer");
    /**
     * {@code "transfer-encoding"}
     */
    public static final String TRANSFER_ENCODING = ("transfer-encoding");
    /**
     * {@code "upgrade"}
     */
    public static final String UPGRADE = ("upgrade");
    /**
     * {@code "upgrade-insecure-requests"}
     */
    public static final String UPGRADE_INSECURE_REQUESTS = ("upgrade-insecure-requests");
    /**
     * {@code "user-agent"}
     */
    public static final String USER_AGENT = ("user-agent");
    /**
     * {@code "vary"}
     */
    public static final String VARY = ("vary");
    /**
     * {@code "via"}
     */
    public static final String VIA = ("via");
    /**
     * {@code "warning"}
     */
    public static final String WARNING = ("warning");
    /**
     * {@code "websocket-location"}
     */
    public static final String WEBSOCKET_LOCATION = ("websocket-location");
    /**
     * {@code "websocket-origin"}
     */
    public static final String WEBSOCKET_ORIGIN = ("websocket-origin");
    /**
     * {@code "websocket-protocol"}
     */
    public static final String WEBSOCKET_PROTOCOL = ("websocket-protocol");
    /**
     * {@code "www-authenticate"}
     */
    public static final String WWW_AUTHENTICATE = ("www-authenticate");
    /**
     * {@code "x-frame-options"}
     */
    public static final String X_FRAME_OPTIONS = ("x-frame-options");
    /**
     * {@code "x-requested-with"}
     */
    public static final String X_REQUESTED_WITH = ("x-requested-with");

    /**
     * {@code "alt-svc"}
     */
    public static final String ALT_SVC = ("alt-svc");

    private HttpHeaderNames() { }
}
