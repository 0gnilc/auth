package com.gnilc.system.i18n.service.impl;

import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.system.i18n.entity.dto.I18nMessageValueDto;
import com.gnilc.system.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.system.i18n.entity.dto.I18nMessageDto;
import com.gnilc.system.i18n.service.DynamicI18nMessageService;
import com.gnilc.system.support.SystemContainerContextInitializer;
import com.gnilc.system.support.SystemTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional
class DynamicI18nMessageServiceIT {

    @Autowired
    private DynamicI18nMessageService messages;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void saveUpdatesAnImmutableIdentityAndPhysicallyRemovesOptionalLocales() {
        messages.saveMessage(save("default", "test.workflow.title",
                value("zh-CN", "  流程  "), value("en-US", "  Workflow  ")));

        assertThat(messages.getMessageValues("test.workflow.title").getValues())
                .extracting(value -> value.getLocale() + ":" + value.getValue())
                .containsExactly("zh-CN:流程", "en-US:Workflow");

        messages.saveMessage(save("admin", "test.workflow.title",
                value("zh-CN", " "), value("en-US", "  Workflow updated  ")));
        assertThat(messages.getMessageValues("test.workflow.title")).satisfies(message -> {
            assertThat(message.getCategory()).isEqualTo("admin");
            assertThat(message.getValues())
                .extracting(value -> value.getLocale() + ":" + value.getValue())
                .containsExactly("en-US:Workflow updated");
        });
        assertThat(jdbc.queryForList("""
                SELECT DISTINCT category FROM sys_i18n
                 WHERE message_key = 'test.workflow.title'
                """, String.class)).containsExactly("admin");

        messages.removeMessage("test.workflow.title");
        messages.removeMessage("test.workflow.title");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_i18n
                 WHERE message_key = 'test.workflow.title'
                """, Integer.class)).isZero();
    }

    @Test
    void bundleAndGroupedPageReturnCompleteNestedMessages() {
        messages.saveMessage(save("admin", "test.page.title",
                value("zh-CN", "页面"), value("en-US", "Page")));
        messages.saveMessage(save("admin", "test.page.subtitle",
                value("zh-CN", "副标题"), value("en-US", "Special subtitle")));

        Map<String, Object> bundle = messages.getMessageBundle("admin");
        @SuppressWarnings("unchecked")
        Map<String, Object> zhCn = (Map<String, Object>) bundle.get("zh-CN");
        @SuppressWarnings("unchecked")
        Map<String, Object> test = (Map<String, Object>) zhCn.get("test");
        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) test.get("page");
        assertThat(page).containsEntry("title", "页面").containsEntry("subtitle", "副标题");

        I18nMessagePageDto query = new I18nMessagePageDto();
        query.setKey("test.page");
        query.setCategory("admin");
        query.setLocale("en-US");
        query.setValue("Special");
        query.setCurrentPage(1L);
        query.setPageSize(1L);
        var result = messages.getMessagePage(query);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getList()).singleElement().satisfies(item -> {
            assertThat(item.getMessageKey()).isEqualTo("test.page.subtitle");
            assertThat(item.getValues()).hasSize(2);
        });
    }

    @Test
    void saveRejectsLeafAndObjectPathConflicts() {
        messages.saveMessage(save("default", "test.conflict",
                value("zh-CN", "冲突"), value("en-US", "Conflict")));

        assertThatThrownBy(() -> messages.saveMessage(save(
                "admin", "test.conflict.child", value("en-US", "Conflict child"))))
                .isInstanceOf(IllegalConditionException.class);

        messages.saveMessage(save("default", "test.reverse.child",
                value("en-US", "Reverse child")));
        assertThatThrownBy(() -> messages.saveMessage(save(
                "admin", "test.reverse", value("en-US", "Reverse parent"))))
                .isInstanceOf(IllegalConditionException.class);
    }

    @Test
    void saveUsesTheExactFourThousandCharacterValueBoundary() {
        String maximum = "v".repeat(4000);
        messages.saveMessage(save("default", "test.value.maximum",
                value("en-US", maximum)));

        assertThat(messages.getMessageValues("test.value.maximum").getValues())
                .singleElement()
                .extracting(value -> value.getValue().length())
                .isEqualTo(4000);
        assertThatThrownBy(() -> messages.saveMessage(save(
                "default", "test.value.oversized", value("en-US", "v".repeat(4001)))))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void concurrentSavesSerializePathConflictValidation() throws Exception {
        String parentKey = "test.concurrent";
        String childKey = "test.concurrent.title";
        CountDownLatch parentSaved = new CountDownLatch(1);
        CountDownLatch releaseParent = new CountDownLatch(1);
        CountDownLatch childStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> parentFuture = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        messages.saveMessage(save("default", parentKey, value("en-US", "Parent")));
                        parentSaved.countDown();
                        await(releaseParent);
                    }));
            assertThat(parentSaved.await(10, TimeUnit.SECONDS)).isTrue();

            Future<?> childFuture = executor.submit(() -> {
                childStarted.countDown();
                messages.saveMessage(save("admin", childKey, value("en-US", "Child")));
            });
            assertThat(childStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> childFuture.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            releaseParent.countDown();
            parentFuture.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> childFuture.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalConditionException.class);
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_i18n
                     WHERE message_key IN (?, ?)
                    """, Integer.class, parentKey, childKey)).isEqualTo(1);
        } finally {
            releaseParent.countDown();
            executor.shutdownNow();
            TransactionTemplate cleanup = new TransactionTemplate(transactionManager);
            cleanup.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            cleanup.executeWithoutResult(status -> jdbc.update("""
                    DELETE FROM sys_i18n
                     WHERE message_key IN (?, ?)
                    """, parentKey, childKey));
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent i18n save");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent i18n save", exception);
        }
    }

    private I18nMessageDto save(
            String category,
            String key,
            I18nMessageValueDto... values) {
        I18nMessageDto dto = new I18nMessageDto();
        dto.setCategory(category);
        dto.setMessageKey(key);
        dto.setValues(List.of(values));
        return dto;
    }

    private I18nMessageValueDto value(String locale, String value) {
        I18nMessageValueDto dto = new I18nMessageValueDto();
        dto.setLocale(locale);
        dto.setValue(value);
        return dto;
    }
}
