package com.gnilc.system.i18n.dao;

import com.gnilc.system.i18n.entity.bo.I18nBo;
import com.gnilc.system.support.SystemContainerContextInitializer;
import com.gnilc.system.support.SystemTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional
class I18nMapperIT {

    @Autowired
    private I18nDao messages;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void mappingUsesCaseSensitiveUniqueKeyAutoFillAndPhysicalDelete() {
        I18nBo lower = message("test.mapper.title", "zh-CN", "标题");
        messages.insert(lower);
        messages.insert(message("test.Mapper.title", "zh-CN", "大写标题"));

        assertThat(lower.getId()).isNotNull();
        assertThat(lower.getCreateTime()).isNotNull();
        assertThatThrownBy(() -> messages.insert(message("test.mapper.title", "zh-CN", "重复")))
                .isInstanceOf(DuplicateKeyException.class);

        messages.deleteById(lower.getId());

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_i18n WHERE id = ?", Integer.class, lower.getId()))
                .isZero();
    }

    private I18nBo message(String key, String locale, String value) {
        I18nBo row = new I18nBo();
        row.setClient("admin");
        row.setI18nKey(key);
        row.setLocale(locale);
        row.setI18nValue(value);
        return row;
    }
}
