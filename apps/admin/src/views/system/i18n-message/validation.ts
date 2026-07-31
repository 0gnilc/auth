export const I18N_MESSAGE_MAX_CODE_POINTS = 4000;

// HTML maxlength counts UTF-16 code units, so every valid code-point sequence
// needs up to twice the business limit when it contains supplementary characters.
export const I18N_MESSAGE_INPUT_MAX_LENGTH = I18N_MESSAGE_MAX_CODE_POINTS * 2;
