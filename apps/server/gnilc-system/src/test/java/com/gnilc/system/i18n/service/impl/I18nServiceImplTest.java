package com.gnilc.system.i18n.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.system.i18n.dao.I18nDao;
import com.gnilc.system.i18n.entity.bo.I18nBo;
import com.gnilc.system.i18n.entity.dto.I18nMessageValueDto;
import com.gnilc.system.i18n.entity.dto.I18nSaveDto;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class I18nServiceImplTest {

    @Mock
    private I18nDao dao;

    private I18nServiceImpl service;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(I18nBo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "i18n-service-test"),
                    I18nBo.class);
        }
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/system/messages");
        messageSource.setDefaultEncoding("UTF-8");
        service = spy(new I18nServiceImpl(new I18nMessageService(messageSource, "zh-CN")));
    }

    @Test
    void getValuesUsesMapperAndReturnsSupportedLocaleOrder() {
        doAnswer(invocation -> new LambdaQueryChainWrapper<>(
                dao, Wrappers.lambdaQuery(I18nBo.class)))
                .when(service).lambdaQuery();
        when(dao.selectList(any())).thenReturn(List.of(
                row("en-US", "Home"),
                row("zh-CN", "首页")));

        var message = service.getValues("admin", "menu.home.title");

        assertThat(message.getI18nKey()).isEqualTo("menu.home.title");
        assertThat(message.getValues())
                .extracting(value -> value.getLocale() + ":" + value.getValue())
                .containsExactly("zh-CN:首页", "en-US:Home");
        verify(dao).selectList(any());
    }

    @Test
    void saveRejectsInvalidInputBeforeDatabaseAccess() {
        I18nSaveDto duplicateLocales = save("menu.home.title",
                value("zh-CN", "首页"), value("zh-CN", "主页"));

        assertThatThrownBy(() -> service.saveMessage("admin", duplicateLocales))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> service.saveMessage("admin", save("menu.__proto__.title")))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> service.saveMessage("unknown", save("menu.home.title")))
                .isInstanceOf(InvalidArgumentException.class);
        verifyNoInteractions(dao);
    }

    private I18nBo row(String locale, String value) {
        I18nBo row = new I18nBo();
        row.setClient("admin");
        row.setI18nKey("menu.home.title");
        row.setLocale(locale);
        row.setI18nValue(value);
        return row;
    }

    private I18nSaveDto save(String key, I18nMessageValueDto... values) {
        I18nSaveDto dto = new I18nSaveDto();
        dto.setI18nKey(key);
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
