package com.gnilc.system.i18n.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.common.utils.PageResult;
import com.gnilc.system.i18n.entity.bo.I18nMessageBo;
import com.gnilc.system.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.system.i18n.entity.dto.I18nMessageDto;
import com.gnilc.system.i18n.entity.vo.I18nMessageVo;
import com.gnilc.system.i18n.entity.vo.I18nMessageItemVo;

import java.util.Map;

public interface DynamicI18nMessageService extends IService<I18nMessageBo> {

    Map<String, Object> getMessageBundle(String client);

    PageResult<I18nMessageItemVo> getMessagePage(String client, I18nMessagePageDto dto);

    I18nMessageVo getMessageValues(String client, String i18nKey);

    I18nMessageVo saveMessage(String client, I18nMessageDto dto);

    void removeMessage(String client, String i18nKey);
}
