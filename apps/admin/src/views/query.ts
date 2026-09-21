import { toUtcInstant } from '@vben/utils';

/** 从本地日期范围转换的 UTC 筛选边界；无法解析或未选择的边界省略。 */
interface UtcRange {
  /** 包含式范围起点，ISO 8601 UTC 时间；未提供或无效时省略。 */
  from?: string;
  /** 包含式范围终点，ISO 8601 UTC 时间；未提供或无效时省略。 */
  to?: string;
}

/** 将界面日期范围按当前显示时区转为 UTC 闭区间；缺失边界保留为省略，不补造整日时间。 */
export function toUtcRange(value: unknown): UtcRange {
  if (!Array.isArray(value)) return {};
  const [from, to] = value;
  return {
    from: typeof from === 'string' ? toUtcInstant(from) : undefined,
    to: typeof to === 'string' ? toUtcInstant(to) : undefined,
  };
}
