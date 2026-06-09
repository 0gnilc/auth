package com.gnilc.authz.rbac.controller;

import com.gnilc.authz.rbac.common.constant.ResponseCode;
import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.entity.dto.MenuDto;
import com.gnilc.authz.rbac.entity.vo.MenuVo;
import com.gnilc.authz.rbac.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuControllerTest {
    private MenuService menuService;
    private MenuController controller;

    /**
     * Sets up fresh controller fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        menuService = mock(MenuService.class);
        controller = new MenuController();
        ReflectionTestUtils.setField(controller, "menuService", menuService);
    }

    /**
     * Verifies the controller returns the service menu tree.
     */
    @Test
    void treeReturnsServiceTree() {
        MenuVo menu = new MenuVo();
        menu.setId(1L);
        when(menuService.getMenuTree()).thenReturn(List.of(menu));

        R<List<MenuVo>> r = controller.getMenuTree();

        assertSuccess(r);
        assertThat(r.getData()).containsExactly(menu);
    }

    /**
     * Verifies menu creation delegates to the service.
     */
    @Test
    void createDelegatesToService() {
        MenuDto dto = new MenuDto();
        dto.setName("system");

        R<?> r = controller.createMenu(dto);

        verify(menuService).createMenu(dto);
        assertSuccess(r);
    }

    /**
     * Verifies menu updates delegate to the service.
     */
    @Test
    void updateDelegatesToService() {
        MenuDto dto = new MenuDto();
        dto.setId(1L);

        R<?> r = controller.updateMenu(dto);

        verify(menuService).updateMenu(dto);
        assertSuccess(r);
    }

    /**
     * Verifies menu removal passes the path ID to the service.
     */
    @Test
    void removeDelegatesPathIdToService() {
        R<?> r = controller.removeMenu(1L);

        verify(menuService).removeMenu(1L);
        assertSuccess(r);
    }

    private void assertSuccess(R<?> r) {
        assertThat(r.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(r.getMessage()).isEqualTo(ResponseCode.SUCCESS.getMessage());
    }
}
