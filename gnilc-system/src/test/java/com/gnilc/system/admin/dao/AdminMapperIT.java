package com.gnilc.system.admin.dao;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.gnilc.auth.authz.rbac.config.MyMetaObjectHandler;
import com.gnilc.system.admin.entity.bo.AdminBo;
import com.gnilc.test.annotation.IntegrationTest;
import com.gnilc.system.support.SystemMySqlContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@MybatisPlusTest(properties = {
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.logic-delete-field=del",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
        "mybatis-plus.global-config.db-config.id-type=auto"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
        classes = AdminMapperIT.MapperTestConfiguration.class,
        initializers = SystemMySqlContainerContextInitializer.class
)
@ImportAutoConfiguration(MybatisPlusAutoConfiguration.class)
class AdminMapperIT {
    @Autowired
    private AdminDao adminDao;

    @Test
    void persistsMapsQueriesUpdatesAndLogicallyDeletesAnAdministrator() {
        AdminBo admin = new AdminBo();
        admin.setUserId(9001L);
        admin.setUsername("mapper-admin");
        admin.setPassword("bcrypt-hash");
        admin.setNickname("Mapper Administrator");
        admin.setDescription("stored description");
        admin.setHomePath("/mapper");
        admin.setStatus(true);

        assertThat(adminDao.insert(admin)).isEqualTo(1);
        assertThat(admin.getId()).isNotNull();
        assertThat(admin.getCreateTime()).isNotNull();

        AdminBo stored = adminDao.selectById(admin.getId());
        assertThat(stored.getUserId()).isEqualTo(9001L);
        assertThat(stored.getUsername()).isEqualTo("mapper-admin");
        assertThat(stored.getDescription()).isEqualTo("stored description");
        assertThat(stored.getStatus()).isTrue();

        stored.setNickname("Updated Administrator");
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        assertThat(adminDao.updateById(stored)).isEqualTo(1);
        AdminBo updated = adminDao.selectById(admin.getId());
        assertThat(updated.getNickname()).isEqualTo("Updated Administrator");
        assertThat(updated.getUpdateTime()).isAfter(beforeUpdate);

        assertThat(adminDao.deleteById(admin.getId())).isEqualTo(1);
        assertThat(adminDao.selectById(admin.getId())).isNull();
    }

    @Test
    void enforcesUniqueUsernameAndUserIdKeys() {
        adminDao.insert(admin(9101L, "unique-admin", "Unique Admin"));

        assertThatThrownBy(() -> adminDao.insert(admin(9102L, "unique-admin", "Duplicate Username")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> adminDao.insert(admin(9101L, "different-admin", "Duplicate User")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void looksUpByUsernameAndUserIdAndExcludesLogicalDeletes() {
        AdminBo expected = admin(9201L, "lookup-admin", "Lookup Admin");
        adminDao.insert(expected);
        adminDao.insert(admin(9202L, "other-admin", "Other Admin"));

        assertThat(adminDao.selectOne(new LambdaQueryWrapper<AdminBo>()
                .eq(AdminBo::getUsername, "lookup-admin"))).extracting(AdminBo::getId)
                .isEqualTo(expected.getId());
        assertThat(adminDao.selectOne(new LambdaQueryWrapper<AdminBo>()
                .eq(AdminBo::getUserId, 9201L))).extracting(AdminBo::getUsername)
                .isEqualTo("lookup-admin");

        adminDao.deleteById(expected.getId());

        assertThat(adminDao.selectOne(new LambdaQueryWrapper<AdminBo>()
                .eq(AdminBo::getUsername, "lookup-admin"))).isNull();
        assertThat(adminDao.selectOne(new LambdaQueryWrapper<AdminBo>()
                .eq(AdminBo::getUserId, 9201L))).isNull();
    }

    @Test
    void pagesFilteredAdministratorsInStableDescendingIdOrder() {
        adminDao.insert(admin(9301L, "page-one", "Support Alpha"));
        adminDao.insert(admin(9302L, "page-two", "Support Beta"));
        adminDao.insert(admin(9303L, "page-three", "Finance"));

        Page<AdminBo> firstPage = adminDao.selectPage(
                Page.of(1, 1),
                new LambdaQueryWrapper<AdminBo>()
                        .like(AdminBo::getNickname, "Support")
                        .orderByDesc(AdminBo::getId));
        Page<AdminBo> secondPage = adminDao.selectPage(
                Page.of(2, 1),
                new LambdaQueryWrapper<AdminBo>()
                        .like(AdminBo::getNickname, "Support")
                        .orderByDesc(AdminBo::getId));

        assertThat(firstPage.getTotal()).isEqualTo(2);
        assertThat(firstPage.getPages()).isEqualTo(2);
        assertThat(firstPage.getRecords()).extracting(AdminBo::getUsername).containsExactly("page-two");
        assertThat(secondPage.getRecords()).extracting(AdminBo::getUsername).containsExactly("page-one");
    }

    private AdminBo admin(Long userId, String username, String nickname) {
        AdminBo admin = new AdminBo();
        admin.setUserId(userId);
        admin.setUsername(username);
        admin.setPassword("bcrypt-hash");
        admin.setNickname(nickname);
        admin.setHomePath("/workspace");
        admin.setStatus(true);
        return admin;
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan("com.gnilc.system.admin.dao")
    static class MapperTestConfiguration {
        @Bean
        MyMetaObjectHandler myMetaObjectHandler() {
            return new MyMetaObjectHandler();
        }

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
            return interceptor;
        }
    }
}
