package com.gnilc.common.constant;

import lombok.Getter;

/**
 * 响应体业务码。
 *  <p>
 *  该 code 只表示 JSON 响应体中的业务结果，不表示 HTTP Status。
 */
@Getter
public enum ResponseCode {
    /** 业务处理成功。 */
    SUCCESS(0, "ok"),
    /** 未归入更具体分类的业务处理错误。 */
    ERROR(10000, "error"),
    /** 请求参数不满足输入契约。 */
    ARGUMENT_INVALID(10001, "argument invalid"),
    /** 当前业务状态或条件不允许执行操作。 */
    ILLEGAL_CONDITION(10002, "illegal condition"),
    /** 请求的资源不存在。 */
    NO_RESOURCE_FOUND(10004, "no resource found"),
    /** 提交的凭证未能通过身份认证。 */
    AUTHENTICATION_FAILED(20001, "authentication failed"),
    /** 请求缺少可用的认证身份或会话。 */
    UNAUTHORIZED(20002, "unauthorized"),
    /** 当前身份不具备访问目标所需的权限。 */
    ACCESS_DENIED(20003, "access denied");

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 响应体中的稳定业务码，不表示 HTTP 状态。 */
    private final Integer code;
    /** 该业务码的默认说明；面向用户的具体错误可由本地化消息替代。 */
    private final String message;
}
