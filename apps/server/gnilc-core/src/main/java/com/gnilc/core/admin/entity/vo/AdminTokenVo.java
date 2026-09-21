package com.gnilc.core.admin.entity.vo;

import lombok.Data;

/** 管理员独立会话的访问及刷新令牌。 */
@Data
public class AdminTokenVo {
    /** 当前身份会话的 Bearer 访问令牌。 */
    private String accessToken;

    /** 当前身份会话的刷新令牌，用于续期该会话。 */
    private String refreshToken;

    /**
     * 创建令牌响应。
     */
    public static AdminTokenVo of(String accessToken, String refreshToken) {
        AdminTokenVo vo = new AdminTokenVo();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        return vo;
    }
}
