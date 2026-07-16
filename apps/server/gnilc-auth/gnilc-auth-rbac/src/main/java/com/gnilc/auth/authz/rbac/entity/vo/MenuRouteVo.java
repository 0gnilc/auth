package com.gnilc.auth.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 后端导航路由。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuRouteVo {
    private String name;
    private String path;
    private String component;
    private String redirect;
    private Meta meta;
    private List<MenuRouteVo> children = new ArrayList<>();

    /**
     * Vben 路由元数据。
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {
        private String activePath;
        private Boolean affixTab;
        private Integer affixTabOrder;
        private String badge;
        private String badgeType;
        private String badgeVariants;
        private Boolean fullPathKey;
        private Boolean hideChildrenInMenu;
        private Boolean hideInBreadcrumb;
        private Boolean hideInMenu;
        private Boolean hideInTab;
        private String icon;
        private String iframeSrc;
        private Boolean ignoreAccess;
        private Boolean keepAlive;
        private String link;
        private Boolean loaded;
        private Integer maxNumOfOpenTab;
        private Boolean menuVisibleWithForbidden;
        private Boolean noBasicLayout;
        private Boolean openInNewWindow;
        private Integer order;
        private Map<String, Object> query;
        private String title;
    }
}
