package com.gnilc.system.i18n.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.utils.R;
import com.gnilc.system.i18n.I18nConstants;
import com.gnilc.system.i18n.entity.dto.I18nKeyDto;
import com.gnilc.system.i18n.entity.dto.I18nPageDto;
import com.gnilc.system.i18n.entity.dto.I18nSaveDto;
import com.gnilc.system.i18n.entity.vo.I18nMessageVo;
import com.gnilc.system.i18n.entity.vo.I18nPageVo;
import com.gnilc.system.i18n.service.I18nService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sys/i18n")
public class I18nController {

    private final I18nService i18nService;

    public I18nController(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    @PostMapping("/bundle")
    public R<Map<String, Object>> getBundle(
            @RequestHeader(value = I18nConstants.CLIENT_HEADER, required = false) String client) {
        return R.success(i18nService.getBundle(client));
    }

    @PostMapping("/page")
    public R<PageResult<I18nPageVo>> getPage(
            @RequestHeader(value = I18nConstants.CLIENT_HEADER, required = false) String client,
            @RequestBody(required = false) I18nPageDto dto) {
        return R.success(i18nService.getPage(client, dto));
    }

    @PostMapping("/values")
    public R<I18nMessageVo> getValues(
            @RequestHeader(value = I18nConstants.CLIENT_HEADER, required = false) String client,
            @Valid @RequestBody I18nKeyDto dto) {
        return R.success(i18nService.getValues(client, dto.getI18nKey()));
    }

    @PostMapping("/save")
    public R<I18nMessageVo> saveMessage(
            @RequestHeader(value = I18nConstants.CLIENT_HEADER, required = false) String client,
            @Valid @RequestBody I18nSaveDto dto) {
        return R.success(i18nService.saveMessage(client, dto));
    }

    @PostMapping("/remove")
    public R<?> removeMessage(
            @RequestHeader(value = I18nConstants.CLIENT_HEADER, required = false) String client,
            @Valid @RequestBody I18nKeyDto dto) {
        i18nService.removeMessage(client, dto.getI18nKey());
        return R.success();
    }
}
