package com.gnilc.core.i18n.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.utils.R;
import com.gnilc.core.i18n.entity.dto.I18nMessageDto;
import com.gnilc.core.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.core.i18n.entity.vo.I18nMessageItemVo;
import com.gnilc.core.i18n.entity.vo.I18nMessageVo;
import com.gnilc.core.i18n.service.DynamicI18nMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

/** 按分类提供运行时动态语言包，按全局消息键提供管理操作。 */
@RestController
@RequestMapping("/sys/i18n-message")
public class I18nMessageController {

    private final DynamicI18nMessageService dynamicI18nMessageService;

    public I18nMessageController(DynamicI18nMessageService dynamicI18nMessageService) {
        this.dynamicI18nMessageService = dynamicI18nMessageService;
    }

    @PostMapping("/bundle/{category}")
    public R<Map<String, Object>> getMessageBundle(
            @PathVariable("category") String category) {
        return R.success(dynamicI18nMessageService.getMessageBundle(category));
    }

    @PostMapping("/page")
    public R<PageResult<I18nMessageItemVo>> getMessagePage(
            @RequestBody I18nMessagePageDto dto) {
        return R.success(dynamicI18nMessageService.getMessagePage(dto));
    }

    @PostMapping("/categories")
    public R<List<String>> getSupportedCategories() {
        return R.success(dynamicI18nMessageService.getSupportedCategories());
    }

    @PostMapping("/values/{messageKey}")
    public R<I18nMessageVo> getMessageValues(
            @PathVariable("messageKey") String messageKey) {
        return R.success(dynamicI18nMessageService.getMessageValues(messageKey));
    }

    @PostMapping("/create")
    public R<I18nMessageVo> createMessage(
            @Valid @RequestBody I18nMessageDto dto) {
        return R.success(dynamicI18nMessageService.createMessage(dto));
    }

    @PostMapping("/save")
    public R<I18nMessageVo> saveMessage(
            @Valid @RequestBody I18nMessageDto dto) {
        return R.success(dynamicI18nMessageService.saveMessage(dto));
    }

    @PostMapping("/remove/{messageKey}")
    public R<?> removeMessage(
            @PathVariable("messageKey") String messageKey) {
        dynamicI18nMessageService.removeMessage(messageKey);
        return R.success();
    }
}
