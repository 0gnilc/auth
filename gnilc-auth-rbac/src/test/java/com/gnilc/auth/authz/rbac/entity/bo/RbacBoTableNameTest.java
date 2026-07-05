package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RbacBoTableNameTest {

    // TestCaseId: RBAC-COMMON-007
    @Test
    void mapsRbacBosToAzPrefixedTables() {
        Map<Class<?>, String> tableNames = Map.of(
                RoleBo.class, "az_role",
                PermissionBo.class, "az_permission",
                MenuBo.class, "az_menu",
                UserBo.class, "az_user",
                UserRoleBo.class, "az_user_role",
                RolePermissionBo.class, "az_role_permission",
                RoleMenuBo.class, "az_role_menu"
        );

        tableNames.forEach((boClass, tableName) -> {
            TableName annotation = boClass.getAnnotation(TableName.class);

            assertThat(annotation)
                    .as("%s should declare @TableName", boClass.getSimpleName())
                    .isNotNull();
            assertThat(annotation.value())
                    .as("%s table name", boClass.getSimpleName())
                    .isEqualTo(tableName);
        });
    }
}
