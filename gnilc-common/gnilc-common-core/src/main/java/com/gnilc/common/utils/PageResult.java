package com.gnilc.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long totalPage;

    private long totalCount;

    private long pageSize;

    private long currentPage;

    private List<T> list;

    public PageResult(List<T> list, long totalCount, long pageSize, long currentPage) {
        this.list = list;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        this.totalPage = (long) Math.ceil((double) totalCount / pageSize);
    }

    public PageResult(IPage<T> page) {
        this.list = page.getRecords();
        this.totalCount = page.getTotal();
        this.pageSize = page.getSize();
        this.currentPage = page.getCurrent();
        this.totalPage = page.getPages();
    }

    public PageResult() {
        this.list = List.of();
        this.totalCount = 0L;
        this.pageSize = 10L;
        this.currentPage = 1L;
        this.totalPage = 0L;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page);
    }

    public static <T> PageResult<T> of(IPage<?> page, List<T> list) {
        return new PageResult<>(list, page.getTotal(), page.getSize(), page.getCurrent());
    }

    public static <T> PageResult<T> of(PageResult<?> page, List<T> list) {
        return new PageResult<>(list, page.getTotalCount(), page.getPageSize(), page.getCurrentPage());
    }
}
