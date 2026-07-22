package com.gnilc.system.i18n.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.utils.R;
import com.gnilc.system.i18n.I18nMessageConstants;
import com.gnilc.system.i18n.entity.dto.I18nMessageDto;
import com.gnilc.system.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.system.i18n.entity.vo.I18nMessageItemVo;
import com.gnilc.system.i18n.entity.vo.I18nMessageVo;
import com.gnilc.system.i18n.service.DynamicI18nMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sys/i18n-message")
public class I18nMessageController {

    private final DynamicI18nMessageService i18nMessageService;

    public I18nMessageController(DynamicI18nMessageService i18nMessageService) {
        this.i18nMessageService = i18nMessageService;
    }

    @PostMapping("/bundle")
    public R<Map<String, Object>> getMessageBundle(
            @RequestHeader(value = I18nMessageConstants.CLIENT_HEADER, required = false) String client) {
        return R.success(i18nMessageService.getMessageBundle(client));
    }

    @PostMapping("/page")
    public R<PageResult<I18nMessageItemVo>> getMessagePage(
            @RequestHeader(value = I18nMessageConstants.CLIENT_HEADER, required = false) String client,
            @RequestBody(required = false) I18nMessagePageDto dto) {
        return R.success(i18nMessageService.getMessagePage(client, dto));
    }

    @PostMapping("/values/{messageKey}")
    public R<I18nMessageVo> getMessageValues(
            @RequestHeader(value = I18nMessageConstants.CLIENT_HEADER, required = false) String client,
            @PathVariable("messageKey") String messageKey) {
        return R.success(i18nMessageService.getMessageValues(client, messageKey));
    }

    @PostMapping("/save")
    public R<I18nMessageVo> saveMessage(
            @RequestHeader(value = I18nMessageConstants.CLIENT_HEADER, required = false) String client,
            @Valid @RequestBody I18nMessageDto dto) {
        return R.success(i18nMessageService.saveMessage(client, dto));
    }

    @PostMapping("/remove/{messageKey}")
    public R<?> removeMessage(
            @RequestHeader(value = I18nMessageConstants.CLIENT_HEADER, required = false) String client,
            @PathVariable("messageKey") String messageKey) {
        i18nMessageService.removeMessage(client, messageKey);
        return R.success();
    }
}
