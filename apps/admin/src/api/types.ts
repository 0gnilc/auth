export interface PageParams {
  currentPage?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  currentPage: number;
  list: T[];
  pageSize: number;
  totalCount: number;
  totalPage: number;
}
