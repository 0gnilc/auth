package com.gnilc.auth.system.admin.entity.bo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class AdminBoTest {

    // TestCaseId: SYS-ADMIN-015
    @Test
    void mapsToSysAdminWithAutoIncrementId() throws NoSuchFieldException {
        TableName tableName = AdminBo.class.getAnnotation(TableName.class);
        Field id = AdminBo.class.getDeclaredField("id");
        TableId tableId = id.getAnnotation(TableId.class);

        assertThat(tableName.value()).isEqualTo("sys_admin");
        assertThat(tableId.type()).isEqualTo(IdType.AUTO);
    }
}
