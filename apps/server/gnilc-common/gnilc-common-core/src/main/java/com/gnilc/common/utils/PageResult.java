package com.gnilc.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gnilc.common.jackson.LongNumberSerializer;
import lombok.Data;

import java.util.List;

/** 分页结果；分页元数据以 JSON 数字返回，列表保留元素自身的传输契约。 */
@Data
public class PageResult<T> {
    /** 按总记录数和每页记录数计算的总页数；空结果为零。 */
    @JsonSerialize(using = LongNumberSerializer.class)
    private long totalPage;

    /** 满足查询条件的总记录数，不限于本页。 */
    @JsonSerialize(using = LongNumberSerializer.class)
    private long totalCount;

    /** 本次查询每页最多返回的记录数。 */
    @JsonSerialize(using = LongNumberSerializer.class)
    private long pageSize;

    /** 本次查询的页码，从 1 开始。 */
    @JsonSerialize(using = LongNumberSerializer.class)
    private long currentPage;

    /** 本页记录列表；默认构造的空结果使用空列表。 */
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
