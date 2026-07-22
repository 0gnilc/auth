/** 单个语言的 Message 文本。 */
export interface I18nMessageValue {
  /** 语言代码，例如 zh-CN。 */
  locale: string;
  /** 当前语言对应的文本。 */
  value: string;
}

/** 一个 Message Key 及其全部语言文本。 */
export interface I18nMessage {
  /** 唯一标识 Message 的点分割 Message Key。 */
  messageKey: string;
  /** 各语言的 Message 文本。 */
  values: I18nMessageValue[];
}

/** 按 Message Key 加载各语言文本，Message Key 不存在时返回 null。 */
export type I18nMessageLoader = (
  messageKey: string,
) => Promise<I18nMessage | null>;

/** 保存 Message 并返回后端确认后的完整数据。 */
export type I18nMessageSaver = (message: I18nMessage) => Promise<I18nMessage>;

/** I18nMessageInput 的公开属性。 */
export interface I18nMessageInputProps {
  /** 外部输入框用于显示文本的默认语言。 */
  defaultLocale?: string;
  /** 是否禁用组件。 */
  disabled?: boolean;
  /** 打开浮层时使用的数据加载函数。 */
  load: I18nMessageLoader;
  /** 浮层内允许编辑的语言列表。 */
  locales?: string[];
  /** 外部输入框为空时的占位文案。 */
  placeholder?: string;
  /** 每个语言文本框的默认行数。 */
  rows?: number;
  /** 点击保存时使用的数据保存函数。 */
  save: I18nMessageSaver;
}
