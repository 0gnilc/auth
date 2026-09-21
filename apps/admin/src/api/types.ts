/** 业务分页输入；省略时使用服务端分页默认值。 */
export interface PageParams {
  /** 页码，从 1 开始。 */
  currentPage?: number;
  /** 每页记录数。 */
  pageSize?: number;
}

/** 业务分页结果；记录类型由调用方指定。 */
export interface PageResult<T> {
  /** 页码，从 1 开始。 */
  currentPage: number;
  /** 当前页记录列表；没有记录时为空数组。 */
  list: T[];
  /** 每页记录数。 */
  pageSize: number;
  /** 匹配条件的总记录数。 */
  totalCount: number;
  /** 匹配条件的总页数。 */
  totalPage: number;
}
