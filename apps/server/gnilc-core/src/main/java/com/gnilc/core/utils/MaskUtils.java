package com.gnilc.core.utils;

/** 提供无状态的展示脱敏能力。 */
public final class MaskUtils {
    /** 禁止实例化脱敏工具。 */
    private MaskUtils() {
    }

    /** 以 Unicode Code Point 脱敏昵称。 */
    public static String maskNickname(String nickname) {
        int[] codePoints = nickname.codePoints().toArray();
        if (codePoints.length <= 1) {
            return "*";
        }
        return new String(codePoints, 0, 1) + "*".repeat(codePoints.length - 1);
    }

    /** 保留本地手机号末四位并附带国家电话区号。 */
    public static String maskPhone(String dialCode, String phone) {
        int visibleDigits = Math.min(4, phone.length());
        String nationalNumber = "*".repeat(phone.length() - visibleDigits)
                + phone.substring(phone.length() - visibleDigits);
        return "+" + dialCode + " " + nationalNumber;
    }
}
