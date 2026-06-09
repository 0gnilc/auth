package com.gnilc.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import com.gnilc.authz.rbac.entity.enums.MenuType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 菜单
 *
 * @author kyhns7
 */
@Data
public class MenuVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 父级ID
     */
    private Long pid;
    /**
     * 菜单类型
     */
    private MenuType type;
    /**
     * 菜单状态,0已禁用、1已启用
     */
    private Boolean status;
    /**
     * 后端权限标识
     */
    private String accessCode;
    /**
     * 菜单名称
     */
    private String name;
    /**
     * 路由路径
     */
    private String path;
    /**
     * 组件
     */
    private String component;
    /**
     * 重定向
     */
    private String redirect;
    /**
     * 指定当前激活的菜单
     */
    private String activePath;
    /**
     * 固定标签页
     */
    private Boolean affixTab;
    /**
     * 固定标签页排序
     */
    private Integer affixTabOrder;
    /**
     * 徽标
     */
    private String badge;
    /**
     * 徽标类型
     */
    private String badgeType;
    /**
     * 徽标样式
     */
    private String badgeVariants;
    /**
     * 是否使用完整路径作为标签页 key
     */
    private Boolean fullPathKey;
    /**
     * 在菜单中隐藏子级
     */
    private Boolean hideChildrenInMenu;
    /**
     * 在面包屑中隐藏
     */
    private Boolean hideInBreadcrumb;
    /**
     * 在菜单中隐藏
     */
    private Boolean hideInMenu;
    /**
     * 在标签页中隐藏
     */
    private Boolean hideInTab;
    /**
     * 图标
     */
    private String icon;
    /**
     * 内嵌 iframe 地址
     */
    private String iframeSrc;
    /**
     * 忽略权限访问控制
     */
    private Boolean ignoreAccess;
    /**
     * 是否缓存页面
     */
    private Boolean keepAlive;
    /**
     * 外链地址
     */
    private String link;
    /**
     * 是否已加载
     */
    private Boolean loaded;
    /**
     * 同名标签页最大打开数量
     */
    private Integer maxNumOfOpenTab;
    /**
     * 菜单可见但访问时跳转 403
     */
    private Boolean menuVisibleWithForbidden;
    /**
     * 不使用基础布局
     */
    private Boolean noBasicLayout;
    /**
     * 在新窗口打开
     */
    private Boolean openInNewWindow;
    /**
     * 排序
     */
    private Integer order;
    /**
     * 路由查询参数
     */
    private String query;
    /**
     * 菜单标题
     */
    private String title;
    /**
     * 子菜单
     */
    private List<MenuVo> children = Lists.newArrayList();
}
