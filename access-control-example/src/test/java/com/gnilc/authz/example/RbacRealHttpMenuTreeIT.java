package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("localtest")
class RbacRealHttpMenuTreeIT extends RbacRealHttpTestSupport {

    /**
     * Verifies all menu types can be created and observed in the tree.
     */
    @Test
    void allMenuTypesCanBeCreatedAndObservedInTreeThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long catalogId = 0L;
        long menuId = 0L;
        long buttonId = 0L;
        long embeddedId = 0L;
        long linkId = 0L;
        try {
            // Create each menu type under one catalog.
            catalogId = createMenu(menuBody(0L, "catalog", "it_menu_catalog_" + suffix,
                    "Catalog " + suffix, "/real/menu/catalog/" + suffix, 1));
            menuId = createMenu(menuBody(catalogId, "menu", "it_menu_page_" + suffix,
                    "Menu " + suffix, "/real/menu/page/" + suffix, 2,
                    "component", "/views/real-menu-page.vue"));
            buttonId = createMenu(menuBody(catalogId, "button", "it_menu_button_" + suffix,
                    "Button " + suffix, null, 3,
                    "accessCode", "real:menu:button:" + suffix));
            embeddedId = createMenu(menuBody(catalogId, "embedded", "it_menu_embedded_" + suffix,
                    "Embedded " + suffix, "/real/menu/embedded/" + suffix, 4,
                    "iframeSrc", "https://example.test/embed/" + suffix));
            linkId = createMenu(menuBody(catalogId, "link", "it_menu_link_" + suffix,
                    "Link " + suffix, null, 5,
                    "link", "https://example.test/link/" + suffix));

            // Verify type-specific fields survive through the tree API.
            JsonNode catalog = menuByName("it_menu_catalog_" + suffix);
            assertThat(catalog.path("type").asText()).isEqualTo("catalog");
            assertThat(catalog.path("path").asText()).isEqualTo("/real/menu/catalog/" + suffix);
            assertThat(findMenuByName(catalog.path("children"), "it_menu_page_" + suffix).path("component").asText())
                    .isEqualTo("/views/real-menu-page.vue");
            assertThat(findMenuByName(catalog.path("children"), "it_menu_button_" + suffix).path("accessCode").asText())
                    .isEqualTo("real:menu:button:" + suffix);
            assertThat(findMenuByName(catalog.path("children"), "it_menu_embedded_" + suffix).path("iframeSrc").asText())
                    .isEqualTo("https://example.test/embed/" + suffix);
            assertThat(findMenuByName(catalog.path("children"), "it_menu_link_" + suffix).path("link").asText())
                    .isEqualTo("https://example.test/link/" + suffix);
            recordScenario("Menu tree / POST /authz/menu/create",
                    "create catalog/menu/button/embedded/link under catalogId=" + catalogId,
                    "POST /api/authz/menu/create for each type, then POST /api/authz/menu/tree",
                    "HTTP 200; tree exposes every menu type with its type-specific fields");
        } finally {
            cleanupQuietly(0L, List.of(), List.of(), List.of(linkId, embeddedId, buttonId, menuId, catalogId));
        }
    }

    /**
     * Verifies menu tree parent-child nesting.
     */
    @Test
    void menuTreeShowsNestedParentChildStructureThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long rootId = 0L;
        long childId = 0L;
        long grandchildId = 0L;
        try {
            rootId = createCatalogMenu("it_menu_root_" + suffix, "/real/menu/root/" + suffix);
            childId = createMenu(menuBody(rootId, "catalog", "it_menu_child_" + suffix,
                    "Child " + suffix, "/real/menu/child/" + suffix, 1));
            grandchildId = createMenu(menuBody(childId, "button", "it_menu_grandchild_" + suffix,
                    "Grandchild " + suffix, null, 1,
                    "accessCode", "real:menu:grandchild:" + suffix));

            JsonNode root = menuByName("it_menu_root_" + suffix);
            JsonNode child = findMenuByName(root.path("children"), "it_menu_child_" + suffix);
            assertThat(child).isNotNull();
            assertThat(longValue(child.path("pid"))).isEqualTo(rootId);
            JsonNode grandchild = findMenuByName(child.path("children"), "it_menu_grandchild_" + suffix);
            assertThat(grandchild).isNotNull();
            assertThat(longValue(grandchild.path("pid"))).isEqualTo(childId);
            recordScenario("Menu tree / POST /authz/menu/tree",
                    "root catalog -> child catalog -> button grandchild, all created through /authz/menu/create",
                    "POST /api/authz/menu/tree",
                    "HTTP 200; tree nests child nodes under their public pid values");
        } finally {
            cleanupQuietly(0L, List.of(), List.of(), List.of(grandchildId, childId, rootId));
        }
    }

    /**
     * Verifies menu tree sibling ordering and null-order placement.
     */
    @Test
    void menuTreeSortsSiblingsByOrderAndNullOrderLastThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long parentId = 0L;
        long firstId = 0L;
        long laterId = 0L;
        long nullOrderId = 0L;
        try {
            parentId = createCatalogMenu("it_menu_sort_parent_" + suffix, "/real/menu/sort-parent/" + suffix);
            laterId = createMenu(menuBody(parentId, "catalog", "it_menu_sort_later_" + suffix,
                    "Sort Later " + suffix, "/real/menu/sort-later/" + suffix, 20));
            nullOrderId = createMenu(menuBody(parentId, "catalog", "it_menu_sort_null_" + suffix,
                    "Sort Null " + suffix, "/real/menu/sort-null/" + suffix, null));
            firstId = createMenu(menuBody(parentId, "catalog", "it_menu_sort_first_" + suffix,
                    "Sort First " + suffix, "/real/menu/sort-first/" + suffix, 1));

            JsonNode children = menuByName("it_menu_sort_parent_" + suffix).path("children");
            int firstIndex = childIndex(children, "it_menu_sort_first_" + suffix);
            int laterIndex = childIndex(children, "it_menu_sort_later_" + suffix);
            int nullIndex = childIndex(children, "it_menu_sort_null_" + suffix);
            assertThat(firstIndex).isLessThan(laterIndex);
            assertThat(laterIndex).isLessThan(nullIndex);
            recordScenario("Menu tree / POST /authz/menu/tree",
                    "three sibling catalogs with order=20, null, and 1 under pid=" + parentId,
                    "POST /api/authz/menu/tree",
                    "HTTP 200; siblings are sorted by order and null order is placed after explicit smaller order values");
        } finally {
            cleanupQuietly(0L, List.of(), List.of(), List.of(firstId, nullOrderId, laterId, parentId));
        }
    }

    /**
     * Verifies menu updates and removals are reflected in the tree.
     */
    @Test
    void menuUpdateAndRemoveAreReflectedInTreeThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long menuId = 0L;
        try {
            String originalName = "it_menu_update_original_" + suffix;
            String updatedName = "it_menu_update_changed_" + suffix;
            menuId = createCatalogMenu(originalName, "/real/menu/update/original/" + suffix);

            postOk("/authz/menu/update", body(
                    "id", menuId,
                    "pid", 0,
                    "type", "catalog",
                    "name", updatedName,
                    "title", "Updated Menu " + suffix,
                    "path", "/real/menu/update/changed/" + suffix,
                    "order", 2
            ));
            assertThat(findMenuByName(postOk("/authz/menu/tree", body()).path("data"), originalName)).isNull();
            JsonNode updated = menuByName(updatedName);
            assertThat(updated.path("path").asText()).isEqualTo("/real/menu/update/changed/" + suffix);

            postOk("/authz/menu/remove/" + menuId, body());
            menuId = 0L;
            assertThat(findMenuByName(postOk("/authz/menu/tree", body()).path("data"), updatedName)).isNull();
            recordScenario("Menu tree / /authz/menu update-remove",
                    "menu created as " + originalName + ", updated to " + updatedName + ", then removed",
                    "POST /api/authz/menu/update -> tree -> POST /api/authz/menu/remove/{id} -> tree",
                    "HTTP 200; tree shows updated fields and then no longer contains the removed menu");
        } finally {
            cleanupQuietly(0L, 0L, 0L, menuId);
        }
    }

    /**
     * Verifies role-menu relations accept IDs from the public tree.
     */
    @Test
    void roleMenuUsesMenuIdsFromPublicTreeThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long roleId = 0L;
        long menuId = 0L;
        try {
            roleId = createRole("it_menu_role_" + suffix, "Menu Role " + suffix);
            menuId = createCatalogMenu("it_menu_role_menu_" + suffix, "/real/menu/role-menu/" + suffix);
            long idFromTree = longValue(menuByName("it_menu_role_menu_" + suffix).path("id"));

            updateRoleMenu(roleId, List.of(idFromTree));
            JsonNode menuIds = postOk("/authz/role-menu/list/" + roleId, body()).path("data");
            assertThat(menuIds).hasSize(1);
            assertThat(longValue(menuIds.get(0))).isEqualTo(menuId);
            recordScenario("Menu relation / /authz/role-menu",
                    "roleId=" + roleId + "; menuId read from /authz/menu/tree=" + idFromTree,
                    "POST /api/authz/role-menu/update then POST /api/authz/role-menu/list/{roleId}",
                    "HTTP 200; role-menu relation accepts the menu ID observed from the public tree API");
        } finally {
            cleanupQuietly(0L, roleId, 0L, menuId);
        }
    }

    private JsonNode menuByName(String name) throws Exception {
        JsonNode menu = findMenuByName(postOk("/authz/menu/tree", body()).path("data"), name);
        assertThat(menu).as("menu named %s", name).isNotNull();
        return menu;
    }

    private int childIndex(JsonNode children, String name) {
        for (int i = 0; i < children.size(); i++) {
            if (name.equals(children.get(i).path("name").asText())) {
                return i;
            }
        }
        throw new AssertionError("Missing child menu " + name);
    }

    private java.util.Map<String, Object> menuBody(long pid, String type, String name, String title,
                                                   String path, Integer order, Object... extraPairs) {
        java.util.Map<String, Object> body = body(
                "pid", pid,
                "type", type,
                "name", name,
                "title", title,
                "order", order
        );
        if (path != null) {
            body.put("path", path);
        }
        for (int i = 0; i < extraPairs.length; i += 2) {
            body.put((String) extraPairs[i], extraPairs[i + 1]);
        }
        return body;
    }
}
