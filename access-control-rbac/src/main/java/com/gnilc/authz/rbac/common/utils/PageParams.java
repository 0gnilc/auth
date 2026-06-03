package com.gnilc.authz.rbac.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class PageParams {
    private final static Integer DEFAULT_CURRENT_PAGE = 1;
    private final static Integer DEFAULT_PAGE_SIZE = 10;

    private Integer currentPage;

    private Integer pageSize;

    public <T> IPage<T> getPage() {
        if (currentPage == null || currentPage < 1) {
            currentPage = DEFAULT_CURRENT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        return new Page<>(currentPage, pageSize);
    }
}
