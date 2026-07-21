package com.gnilc.system.i18n.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.common.utils.PageResult;
import com.gnilc.system.i18n.entity.bo.I18nBo;
import com.gnilc.system.i18n.entity.dto.I18nPageDto;
import com.gnilc.system.i18n.entity.dto.I18nSaveDto;
import com.gnilc.system.i18n.entity.vo.I18nMessageVo;
import com.gnilc.system.i18n.entity.vo.I18nPageVo;

import java.util.Map;

public interface I18nService extends IService<I18nBo> {

    Map<String, Object> getBundle(String client);

    PageResult<I18nPageVo> getPage(String client, I18nPageDto dto);

    I18nMessageVo getValues(String client, String i18nKey);

    I18nMessageVo saveMessage(String client, I18nSaveDto dto);

    void removeMessage(String client, String i18nKey);
}
