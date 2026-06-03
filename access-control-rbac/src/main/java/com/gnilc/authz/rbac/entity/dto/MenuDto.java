package com.gnilc.authz.rbac.entity.dto;

import com.gnilc.authz.rbac.common.constant.MenuConstant;
import lombok.Data;


/**
 * 菜单<br>
 * <a href="https://pure-admin.cn/pages/routerMenu/">具体参考</a>
 * @author kyhns7
 */
@Data
public class MenuDto {

	/**
	 * id
	 */
	private Long id;

	/**
	 * 上级id
	 */
	private Long parentId;

	/**
	 * 类型
	 * @see MenuConstant#TYPE_MENU 菜单
	 * @see MenuConstant#TYPE_IFRAME iframe
	 * @see MenuConstant#TYPE_EXTERNAL_LINK 外链
	 * @see MenuConstant#TYPE_BUTTON 按钮
	 */
	private Integer type;
	/**
	 * 标识,如果是菜单则与路径一致,如果是按钮则是权限标识
	 */
	private String symbol;

	/**
	 * 路由路径
	 */
	private String path;

	/**
	 * 路由名称
	 */
	private String name;

	/**
	 * 排序，数值越小越靠前
	 */
	private Integer sort;

	/**
	 * 路由重定向
	 */
	private String redirect;

	/**
	 * 按需加载需要展示的页面
	 */
	private String component;

	/**
	 * 菜单名称
	 */
	private String title;

	/**
	 * 菜单图标
	 */
	private String icon;

	/**
	 * 菜单名称右侧的额外图标
	 */
	private String extraIcon;

	/**
	 * 是否显示该菜单
	 */
	private Boolean showLink;

	/**
	 * 是否显示父级菜单
	 */
	private Boolean showParent;

	/**
	 * 是否缓存该路由页面（开启后，会保存该页面的整体状态，刷新后会清空状态）
	 */
	private Boolean keepAlive;

	/**
	 * 内嵌的iframe链接地址
	 */
	private String frameSrc;

}
