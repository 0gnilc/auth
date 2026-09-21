package com.gnilc.common.utils;

/** 只判断格式、不修改输入原文的字符串规则。 */
public final class StringRules {
    private StringRules() {
    }

    /**
     * 校验非空文本的 Unicode 码点长度；在 {@link Character#isWhitespace(int)} 识别的空白中，
     * 只允许文本内部单个 ASCII 空格。控制字符和格式字符会被拒绝，其他字符保持原样接受，
     * 包括该谓词不识别为普通空白的不换行空格。
     */
    public static boolean isSingleSpacedText(String value, int maxCodePoints) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int[] codePoints = value.codePoints().toArray();
        if (codePoints.length > maxCodePoints) {
            return false;
        }
        for (int index = 0; index < codePoints.length; index++) {
            int codePoint = codePoints[index];
            int type = Character.getType(codePoint);
            if (type == Character.CONTROL || type == Character.FORMAT) {
                return false;
            }
            if (Character.isWhitespace(codePoint)
                    && (codePoint != ' '
                    || index == 0
                    || index == codePoints.length - 1
                    || codePoints[index - 1] == ' ')) {
                return false;
            }
        }
        return true;
    }

    /**
     * 按 {@link Character#isWhitespace(int)} 判断任一端是否为空白；null 或空串返回 false。
     */
    public static boolean hasEdgeWhitespace(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int first = value.codePointAt(0);
        int last = value.codePointBefore(value.length());
        return Character.isWhitespace(first) || Character.isWhitespace(last);
    }
}
