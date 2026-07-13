package com.gnilc.common.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {
    @Test
    void calculatesTotalPages() {
        PageResult<String> result = new PageResult<>(List.of("a", "b"), 11, 5, 2);

        assertThat(result.getTotalPage()).isEqualTo(3);
        assertThat(result.getCurrentPage()).isEqualTo(2);
    }
}
