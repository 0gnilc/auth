package com.gnilc.auth.authz.rbac.entity.dto;

import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import lombok.Data;


/** 创建或更新菜单的输入；更新时必须指定菜单 ID，已有菜单类型不能改变。 */
@Data
public class MenuDto {

    /** 更新目标的数据库 ID；创建操作不使用该值。 */
    private Long id;

    /** 父菜单 ID，零表示根节点。 */
    private Long pid;

    /** 菜单类型；决定导航配置要求，持久化后不可变更。 */
    private MenuType type;

    /** 菜单是否启用；禁用时不参与可见导航。 */
    private Boolean status;

    /** 菜单操作的前端访问码，控制可见性，不代替后端目标权限检查。 */
    private String accessCode;

    /** 唯一导航路由名称，用于路由身份与页面缓存标识。 */
    private String name;

    /** 前端路由路径；页面、内嵌和外链菜单需要提供。 */
    private String path;

    /** 页面映射所使用的前端组件路径。 */
    private String component;

    /** 可选的导航重定向路径。 */
    private String redirect;

    /** 打开该路由时需要高亮的菜单路径。 */
    private String activePath;

    /** 是否将该路由固定在标签栏。 */
    private Boolean affixTab;

    /** 固定标签页的排序值。 */
    private Integer affixTabOrder;

    /** 菜单徽标显示文本。 */
    private String badge;

    /** 菜单徽标的显示类型。 */
    private String badgeType;

    /** 菜单徽标的视觉样式标识。 */
    private String badgeVariants;

    /** 是否以包含查询参数的完整路径区分标签页。 */
    private Boolean fullPathKey;

    /** 是否在菜单导航中隐藏子级。 */
    private Boolean hideChildrenInMenu;

    /** 是否在面包屑导航中隐藏该项。 */
    private Boolean hideInBreadcrumb;

    /** 是否在菜单导航中隐藏该项。 */
    private Boolean hideInMenu;

    /** 是否在标签栏中隐藏该项。 */
    private Boolean hideInTab;

    /** 菜单显示的图标标识。 */
    private String icon;

    /** 内嵌页面的 HTTP 或 HTTPS 地址。 */
    private String iframeSrc;

    /** 是否缓存对应页面实例。 */
    private Boolean keepAlive;

    /** 外链菜单目标的 HTTP 或 HTTPS 地址。 */
    private String link;

    /** 同一路由名称允许同时打开的标签页数量上限。 */
    private Integer maxNumOfOpenTab;

    /** 是否跳过前端基础布局。 */
    private Boolean noBasicLayout;

    /** 是否在新窗口打开目标。 */
    private Boolean openInNewWindow;

    /** 菜单显示排序值。 */
    private Integer order;

    /** 导航时附带的查询参数，以 JSON 对象文本保存。 */
    private String query;

    /** 菜单显示标题，可使用对应的本地化消息键。 */
    private String title;
}
