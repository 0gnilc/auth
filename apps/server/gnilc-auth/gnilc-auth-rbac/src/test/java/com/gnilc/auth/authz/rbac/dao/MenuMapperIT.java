package com.gnilc.auth.authz.rbac.dao;

import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.support.RbacContainerContextInitializer;
import com.gnilc.auth.authz.rbac.support.RbacTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RbacTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RbacContainerContextInitializer.class)
@Transactional
class MenuMapperIT {
    @Autowired
    private MenuDao menus;

    @Test
    void subtreeQueryTraversesThroughLogicallyDeletedMenuNodes() {
        MenuBo root = menu("Root", "/root", 0L);
        menus.insert(root);
        MenuBo deletedBridge = menu("DeletedBridge", "/root/bridge", root.getId());
        menus.insert(deletedBridge);
        MenuBo activeLeaf = menu("ActiveLeaf", "/root/bridge/leaf", deletedBridge.getId());
        menus.insert(activeLeaf);
        menus.deleteById(deletedBridge.getId());

        assertThat(menus.selectCompleteSubtreeIds(root.getId()))
                .containsExactlyInAnyOrder(root.getId(), deletedBridge.getId(), activeLeaf.getId());
        assertThat(menus.selectCompleteSubtreeIds(Long.MAX_VALUE)).isEmpty();
    }

    private MenuBo menu(String name, String path, Long pid) {
        MenuBo menu = new MenuBo();
        menu.setPid(pid);
        menu.setType(MenuType.MENU);
        menu.setStatus(true);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent("/dashboard/index");
        menu.setOrder(1);
        menu.setTitle(name);
        return menu;
    }
}
