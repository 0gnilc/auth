package com.gnilc.system.i18n.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.system.i18n.entity.dto.I18nPageDto;
import com.gnilc.system.i18n.entity.dto.I18nDto;
import com.gnilc.system.i18n.entity.vo.I18nValueVo;
import com.gnilc.system.i18n.entity.vo.I18nValuesVo;
import com.gnilc.system.i18n.entity.vo.I18nItemVo;
import com.gnilc.system.i18n.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.hamcrest.Matchers.hasItems;

class I18nControllerTest {

    private final I18nService service = mock(I18nService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/common/messages", "i18n/system/messages");
        messageSource.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(messageSource, "zh-CN");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.afterPropertiesSet();
        RestExceptionHandlingConfiguration exceptionHandling =
                new RestExceptionHandlingConfiguration();
        mvc = MockMvcBuilders.standaloneSetup(new I18nController(service))
                .setControllerAdvice(
                        new RestExceptionHandlingConfiguration.RestExceptionControllerAdvice(messages))
                .setLocaleResolver(exceptionHandling.localeResolver("zh-CN"))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void readRoutesReturnGroupedMessagesForRequestClient() throws Exception {
        I18nValuesVo message = message("menu.dashboard.title", "首页", "Dashboard");
        when(service.getBundle("admin"))
                .thenReturn(Map.of("zh-CN", Map.of("menu", Map.of("title", "首页"))));
        when(service.getPage(eq("admin"), any(I18nPageDto.class)))
                .thenReturn(new PageResult<>(
                        List.of(new I18nItemVo("admin", message.getI18nKey(), message.getValues())),
                        1,
                        10,
                        1));
        when(service.getValues("admin", message.getI18nKey())).thenReturn(message);

        mvc.perform(post("/sys/i18n/bundle").header("X-Client", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zh-CN.menu.title").value("首页"));
        mvc.perform(post("/sys/i18n/page")
                        .header("X-Client", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"dashboard\",\"currentPage\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].client").value("admin"))
                .andExpect(jsonPath("$.data.list[0].values[1].locale").value("en-US"));
        mvc.perform(post("/sys/i18n/values/menu.dashboard.title")
                        .header("X-Client", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.i18nKey").value("menu.dashboard.title"));

        verify(service).getBundle("admin");
        verify(service).getValues("admin", "menu.dashboard.title");
    }

    @Test
    void saveAndRemoveRoutesDelegateUnifiedCommands() throws Exception {
        I18nValuesVo saved = message("menu.home.title", "首页", "Home");
        when(service.saveMessage(eq("admin"), any(I18nDto.class))).thenReturn(saved);

        mvc.perform(post("/sys/i18n/save")
                        .header("X-Client", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "previousKey": "menu.old.title",
                                  "i18nKey": "menu.home.title",
                                  "values": [{"locale":"zh-CN","value":"首页"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.i18nKey").value("menu.home.title"));
        mvc.perform(post("/sys/i18n/remove/menu.home.title")
                        .header("X-Client", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(service).saveMessage(eq("admin"), any(I18nDto.class));
        verify(service).removeMessage("admin", "menu.home.title");
    }

    @Test
    void invalidNestedRequestFieldsReturnLocalizedFieldErrorsWithoutCallingService() throws Exception {
        mvc.perform(post("/sys/i18n/save")
                        .header("X-Client", "admin")
                        .header(ACCEPT_LANGUAGE, "en-US-POSIX")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "i18nKey": "",
                                  "values": [{"locale":"","value":"title"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data[*].field", hasItems("i18nKey", "values[0].locale")))
                .andExpect(jsonPath("$.data[*].code", hasItems("NotBlank", "NotBlank")))
                .andExpect(jsonPath("$.data[*].message", hasItems(
                        "国际化 key 不能为空。", "语种不能为空。")));

        verifyNoInteractions(service);
    }

    @Test
    void malformedAndBusinessInvalidRequestsUseTheExistingErrorEnvelope() throws Exception {
        mvc.perform(post("/sys/i18n/save")
                        .header("X-Client", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("请求体格式错误。"));

        when(service.saveMessage(eq("admin"), any(I18nDto.class)))
                .thenThrow(new InvalidArgumentException("invalid key"));
        mvc.perform(post("/sys/i18n/save")
                        .header("X-Client", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"i18nKey\":\"menu.title\",\"values\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("invalid key"));
    }

    private I18nValuesVo message(String key, String zhCn, String enUs) {
        return new I18nValuesVo(key, List.of(
                new I18nValueVo("zh-CN", zhCn),
                new I18nValueVo("en-US", enUs)));
    }
}
