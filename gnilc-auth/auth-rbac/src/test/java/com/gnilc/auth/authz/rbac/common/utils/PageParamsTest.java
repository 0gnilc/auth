package com.gnilc.auth.authz.rbac.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageParamsTest {
    @Test
    void normalizesInvalidValues() {
        PageParams params = new PageParams();
        params.setCurrentPage(0L);
        params.setPageSize(-1L);

        IPage<String> page = params.getPage();

        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);
    }
}
