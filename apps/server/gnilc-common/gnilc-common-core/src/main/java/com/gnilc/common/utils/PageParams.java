package com.gnilc.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/** 分页查询的公共输入；调用 {@link #getPage()} 时规范化缺省值和范围。 */
@Data
public class PageParams {
    private final static Long DEFAULT_CURRENT_PAGE = 1L;
    private final static Long DEFAULT_PAGE_SIZE = 10L;
    private final static Long MAX_PAGE_SIZE = 200L;

    /** 从 1 开始的页码；未提供或小于 1 时取 1，超大页码按页大小限制以避免偏移量溢出。 */
    private Long currentPage;

    /** 每页记录数；未提供或小于 1 时取 10，大于 200 时截为 200。 */
    private Long pageSize;

    /** 规范化当前对象的分页参数后生成查询分页，限制页码以避免偏移量计算溢出。 */
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
