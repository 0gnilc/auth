package com.gnilc.authz.rbac.common.constant;

import java.util.Arrays;
import java.util.List;

public class MenuConstant {
    // 根节点pid
    public static final long ROOT_PARENT_ID = 0L;
    // 菜单
    public static final int TYPE_MENU = 0;
    // iframe
    public static final int TYPE_IFRAME = 1;
    // 外部链接
    public static final int TYPE_EXTERNAL_LINK = 2;
    // 按钮
    public static final int TYPE_BUTTON = 3;
    // 所有类型
    public static final List<Integer> TYPES = Arrays.asList(TYPE_MENU, TYPE_IFRAME,
            TYPE_EXTERNAL_LINK, TYPE_BUTTON);
}
