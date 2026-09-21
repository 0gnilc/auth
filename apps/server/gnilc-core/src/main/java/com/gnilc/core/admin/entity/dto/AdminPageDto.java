package com.gnilc.core.admin.entity.dto;

import com.gnilc.common.utils.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 后台管理员分页查询的可选筛选条件。 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AdminPageDto extends PageParams {
    /** 精确匹配的可选登录用户名；空白值不筛选。 */
    private String username;

    /** 模糊匹配的可选昵称；空白值不筛选。 */
    private String nickname;

    /** 精确匹配的启用状态；null 表示不筛选。 */
    private Boolean status;
}
