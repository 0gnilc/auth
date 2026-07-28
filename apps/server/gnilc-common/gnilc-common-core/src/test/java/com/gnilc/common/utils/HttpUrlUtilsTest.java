package com.gnilc.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUrlUtilsTest {

    @Test
    void acceptsCompleteHttpAndHttpsUrls() {
        assertThat(HttpUrlUtils.isValid("http://example.com")).isTrue();
        assertThat(HttpUrlUtils.isValid("https://example.com/path?q=value#section")).isTrue();
        assertThat(HttpUrlUtils.isValid("HTTPS://localhost:8443/path")).isTrue();
        assertThat(HttpUrlUtils.isValid("http://[::1]/health")).isTrue();
    }

    @Test
    void rejectsUnsupportedOrIncompleteUrls() {
        assertThat(HttpUrlUtils.isValid(null)).isFalse();
        assertThat(HttpUrlUtils.isValid(" ")).isFalse();
        assertThat(HttpUrlUtils.isValid("/relative/path")).isFalse();
        assertThat(HttpUrlUtils.isValid("ftp://example.com/file")).isFalse();
        assertThat(HttpUrlUtils.isValid("https:///missing-host")).isFalse();
        assertThat(HttpUrlUtils.isValid("https://example.com:invalid/path")).isFalse();
        assertThat(HttpUrlUtils.isValid("https://example.com/a path")).isFalse();
    }
}
