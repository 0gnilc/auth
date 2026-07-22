package com.gnilc.system.i18n.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.common.utils.PageResult;
import com.gnilc.system.i18n.entity.bo.I18nBo;
import com.gnilc.system.i18n.entity.dto.I18nPageDto;
import com.gnilc.system.i18n.entity.dto.I18nDto;
import com.gnilc.system.i18n.entity.vo.I18nValuesVo;
import com.gnilc.system.i18n.entity.vo.I18nItemVo;

import java.util.Map;

public interface I18nService extends IService<I18nBo> {

    Map<String, Object> getBundle(String client);

    PageResult<I18nItemVo> getPage(String client, I18nPageDto dto);

    I18nValuesVo getValues(String client, String i18nKey);

    I18nValuesVo saveMessage(String client, I18nDto dto);

    void removeMessage(String client, String i18nKey);
}
