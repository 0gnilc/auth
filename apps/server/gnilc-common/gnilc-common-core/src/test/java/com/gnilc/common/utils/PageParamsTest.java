package com.gnilc.common.utils;

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

    @Test
    void capsPageSizeAtTheLargestSupportedUiOption() {
        PageParams exactMaximum = new PageParams();
        exactMaximum.setPageSize(200L);
        PageParams oversized = new PageParams();
        oversized.setPageSize(Long.MAX_VALUE);

        assertThat(exactMaximum.getPage().getSize()).isEqualTo(200);
        assertThat(oversized.getPage().getSize()).isEqualTo(200);
    }

    @Test
    void capsCurrentPageBeforeThePaginationOffsetCanOverflow() {
        PageParams params = new PageParams();
        params.setCurrentPage(Long.MAX_VALUE);
        params.setPageSize(200L);

        IPage<String> page = params.getPage();

        assertThat(page.getCurrent()).isEqualTo(Long.MAX_VALUE / 200 + 1);
        assertThat(page.offset()).isPositive();
    }
}
