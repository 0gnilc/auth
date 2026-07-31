package com.gnilc.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class PageParams {
    private final static Long DEFAULT_CURRENT_PAGE = 1L;
    private final static Long DEFAULT_PAGE_SIZE = 10L;
    private final static Long MAX_PAGE_SIZE = 200L;

    private Long currentPage;

    private Long pageSize;

    public <T> IPage<T> getPage() {
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        if (currentPage == null || currentPage < 1) {
            currentPage = DEFAULT_CURRENT_PAGE;
        } else {
            long maximumCurrentPage = Long.MAX_VALUE / pageSize;
            if (maximumCurrentPage < Long.MAX_VALUE) {
                maximumCurrentPage++;
            }
            currentPage = Math.min(currentPage, maximumCurrentPage);
        }
        return new Page<>(currentPage, pageSize);
    }
}
