package com.gnilc.system.i18n.service.impl;

import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.system.i18n.entity.dto.I18nValueDto;
import com.gnilc.system.i18n.entity.dto.I18nPageDto;
import com.gnilc.system.i18n.entity.dto.I18nDto;
import com.gnilc.system.i18n.service.I18nService;
import com.gnilc.system.support.SystemContainerContextInitializer;
import com.gnilc.system.support.SystemTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional
class I18nServiceIT {

    @Autowired
    private I18nService messages;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void saveSupportsPartialUpdatesKeyMigrationAndPhysicalRemoval() {
        messages.saveMessage("admin", save("test.workflow.title", null,
                value("zh-CN", "流程")));
        messages.saveMessage("admin", save("test.workflow.title", null,
                value("en-US", "Workflow")));

        assertThat(messages.getValues("admin", "test.workflow.title").getValues())
                .extracting(value -> value.getLocale() + ":" + value.getValue())
                .containsExactly("zh-CN:流程", "en-US:Workflow");

        var migrated = messages.saveMessage("admin", save(
                "test.workflow.heading",
                "test.workflow.title",
                value("zh-CN", "工作流")));
        assertThat(migrated.getI18nKey()).isEqualTo("test.workflow.heading");
        assertThat(migrated.getValues())
                .extracting(value -> value.getLocale() + ":" + value.getValue())
                .containsExactly("zh-CN:工作流", "en-US:Workflow");
        assertThat(messages.getValues("admin", "test.workflow.title")).isNull();

        messages.saveMessage("admin", save("test.workflow.heading", null,
                value("zh-CN", " ")));
        assertThat(messages.getValues("admin", "test.workflow.heading").getValues())
                .extracting(value -> value.getLocale())
                .containsExactly("en-US");

        messages.removeMessage("admin", "test.workflow.heading");
        messages.removeMessage("admin", "test.workflow.heading");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_i18n
                 WHERE client = 'admin' AND i18n_key = 'test.workflow.heading'
                """, Integer.class)).isZero();
    }

    @Test
    void bundleAndGroupedPageReturnCompleteNestedMessages() {
        messages.saveMessage("admin", save("test.page.title", null,
                value("zh-CN", "页面"), value("en-US", "Page")));
        messages.saveMessage("admin", save("test.page.subtitle", null,
                value("zh-CN", "副标题"), value("en-US", "Special subtitle")));

        Map<String, Object> bundle = messages.getBundle("admin");
        @SuppressWarnings("unchecked")
        Map<String, Object> zhCn = (Map<String, Object>) bundle.get("zh-CN");
        @SuppressWarnings("unchecked")
        Map<String, Object> test = (Map<String, Object>) zhCn.get("test");
        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) test.get("page");
        assertThat(page).containsEntry("title", "页面").containsEntry("subtitle", "副标题");

        I18nPageDto query = new I18nPageDto();
        query.setKey("test.page");
        query.setLocale("en-US");
        query.setValue("Special");
        query.setCurrentPage(1L);
        query.setPageSize(1L);
        var result = messages.getPage("admin", query);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getList()).singleElement().satisfies(item -> {
            assertThat(item.getI18nKey()).isEqualTo("test.page.subtitle");
            assertThat(item.getValues()).hasSize(2);
        });
    }

    @Test
    void saveRejectsLeafAndObjectPathConflicts() {
        messages.saveMessage("admin", save("test.conflict", null,
                value("zh-CN", "冲突")));

        assertThatThrownBy(() -> messages.saveMessage("admin", save(
                "test.conflict.child", null, value("en-US", "Conflict"))))
                .isInstanceOf(IllegalConditionException.class);
    }

    private I18nDto save(
            String key,
            String previousKey,
            I18nValueDto... values) {
        I18nDto dto = new I18nDto();
        dto.setI18nKey(key);
        dto.setPreviousKey(previousKey);
        dto.setValues(List.of(values));
        return dto;
    }

    private I18nValueDto value(String locale, String value) {
        I18nValueDto dto = new I18nValueDto();
        dto.setLocale(locale);
        dto.setValue(value);
        return dto;
    }
}
