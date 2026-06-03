package com.gnilc.authz.rbac.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private Integer totalPage;

    private Integer totalCount;

    private Integer pageSize;

    private Integer currentPage;

    private List<T> list;

    public PageResult(List<T> list, int totalCount, int pageSize, int currentPage) {
        this.list = list;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
    }

    public PageResult(IPage<T> page) {
        this.list = page.getRecords();
        this.totalCount = (int) page.getTotal();
        this.pageSize = (int) page.getSize();
        this.currentPage = (int) page.getCurrent();
        this.totalPage = (int) page.getPages();
    }

    public PageResult() {
        this.list = List.of();
        this.totalCount = 0;
        this.pageSize = 10;
        this.currentPage = 1;
        this.totalPage = 0;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page);
    }

    public static <T> PageResult<T> of(IPage<?> page, List<T> list) {
        return new PageResult<>(list, (int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent());
    }

    public static <T> PageResult<T> of(PageResult<?> page, List<T> list) {
        return new PageResult<>(list, page.getTotalCount(), page.getPageSize(), page.getCurrentPage());
    }
}
