export const I18N_MESSAGE_MAX_CODE_POINTS = 4000;

// HTML maxlength 按 UTF-16 代码单元计数；补充平面字符占两个单元，控件上限需容纳业务允许的码点数。
export const I18N_MESSAGE_INPUT_MAX_LENGTH = I18N_MESSAGE_MAX_CODE_POINTS * 2;
