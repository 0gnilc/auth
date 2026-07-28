package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;


/**
 * 菜单
 *
 * @author kyhns7
 */
@Data
@TableName("az_menu")
public class MenuBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 是否删除,0未删除、1已删除
     */
    private Integer del;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;
    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private Instant updateTime;

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
     * 是否系统内置,0否、1是
     */
    private Boolean builtIn;

    /**
     * 后端权限标识
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String accessCode;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 路由路径
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String path;

    /**
     * 组件
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String component;

    /**
     * 重定向
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String redirect;

    /**
     * 指定当前激活的菜单
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String activePath;

    /**
     * 固定标签页
     */
    private Boolean affixTab;

    /**
     * 固定标签页排序
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer affixTabOrder;

    /**
     * 徽标
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String badge;

    /**
     * 徽标类型
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String badgeType;

    /**
     * 徽标样式
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
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
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String icon;

    /**
     * 内嵌 iframe 地址
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String iframeSrc;

    /**
     * 是否缓存页面
     */
    private Boolean keepAlive;

    /**
     * 外链地址
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String link;

    /**
     * 同名标签页最大打开数量
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxNumOfOpenTab;

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
    @TableField("`order`")
    private Integer order;

    /**
     * 路由查询参数
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String query;

    /**
     * 菜单标题
     */
    private String title;
}
