package com.gnilc.common.utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * HTTP URL 工具。
 */
public final class HttpUrlUtils {

    private HttpUrlUtils() {
    }

    /**
     * 判断字符串是否为包含主机名的完整 HTTP 或 HTTPS URL。
     *
     * @param value 待校验的字符串
     * @return URL 协议、主机和语法均有效时返回 {@code true}
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value).parseServerAuthority();
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
