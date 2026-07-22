package com.gnilc.system.i18n.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.i18n.SupportedLocale;
import com.gnilc.common.utils.PageResult;
import com.gnilc.system.i18n.I18nMessageConstants;
import com.gnilc.system.i18n.dao.I18nMessageDao;
import com.gnilc.system.i18n.entity.bo.I18nMessageBo;
import com.gnilc.system.i18n.entity.dto.I18nMessageValueDto;
import com.gnilc.system.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.system.i18n.entity.dto.I18nMessageDto;
import com.gnilc.system.i18n.entity.vo.I18nMessageValueVo;
import com.gnilc.system.i18n.entity.vo.I18nMessageVo;
import com.gnilc.system.i18n.entity.vo.I18nMessageItemVo;
import com.gnilc.system.i18n.service.DynamicI18nMessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DynamicI18nMessageServiceImpl extends ServiceImpl<I18nMessageDao, I18nMessageBo> implements DynamicI18nMessageService {

    private static final int MAX_KEY_LENGTH = 191;
    private static final int MAX_VALUE_LENGTH = 4000;
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*$");
    private static final Set<String> FORBIDDEN_SEGMENTS = Set.of(
            "__proto__", "prototype", "constructor");

    private final I18nMessageService messages;

    public DynamicI18nMessageServiceImpl(I18nMessageService messages) {
        this.messages = messages;
    }

    @Override
    public Map<String, Object> getMessageBundle(String client) {
        String targetClient = requireClient(client);
        List<I18nMessageBo> rows = lambdaQuery()
                .eq(I18nMessageBo::getClient, targetClient)
                .orderByAsc(I18nMessageBo::getI18nKey)
                .list();
        Map<String, Object> bundle = new LinkedHashMap<>();
        for (String locale : SupportedLocale.codes()) {
            Map<String, Object> localeMessages = new LinkedHashMap<>();
            rows.stream()
                    .filter(row -> locale.equals(row.getLocale()))
                    .forEach(row -> putPath(localeMessages, row.getI18nKey(), row.getI18nValue()));
            bundle.put(locale, localeMessages);
        }
        return bundle;
    }

    @Override
    public PageResult<I18nMessageItemVo> getMessagePage(String client, I18nMessagePageDto dto) {
        String targetClient = requireClient(client);
        I18nMessagePageDto query = dto == null ? new I18nMessagePageDto() : dto;
        if (StringUtils.isNotBlank(query.getClient())) {
            Preconditions.checkArgument(targetClient.equals(query.getClient()),
                    messages.get("system.i18n.client.mismatch"));
        }
        if (StringUtils.isNotBlank(query.getLocale())) {
            requireLocale(query.getLocale());
        }

        IPage<I18nMessageBo> keyPage = lambdaQuery()
                .select(I18nMessageBo::getClient, I18nMessageBo::getI18nKey)
                .eq(I18nMessageBo::getClient, targetClient)
                .like(StringUtils.isNotBlank(query.getKey()), I18nMessageBo::getI18nKey, query.getKey())
                .like(StringUtils.isNotBlank(query.getValue()), I18nMessageBo::getI18nValue, query.getValue())
                .eq(StringUtils.isNotBlank(query.getLocale()), I18nMessageBo::getLocale, query.getLocale())
                .groupBy(I18nMessageBo::getClient, I18nMessageBo::getI18nKey)
                .orderByAsc(I18nMessageBo::getI18nKey)
                .page(query.getPage());
        List<String> keys = keyPage.getRecords().stream().map(I18nMessageBo::getI18nKey).toList();
        if (keys.isEmpty()) {
            return PageResult.of(keyPage, List.of());
        }

        Map<String, List<I18nMessageBo>> rowsByKey = lambdaQuery()
                .eq(I18nMessageBo::getClient, targetClient)
                .in(I18nMessageBo::getI18nKey, keys)
                .list()
                .stream()
                .collect(Collectors.groupingBy(I18nMessageBo::getI18nKey));
        List<I18nMessageItemVo> items = keys.stream()
                .map(key -> new I18nMessageItemVo(
                        targetClient,
                        key,
                        toValues(rowsByKey.getOrDefault(key, List.of()))))
                .toList();
        return PageResult.of(keyPage, items);
    }

    @Override
    public I18nMessageVo getMessageValues(String client, String i18nKey) {
        String targetClient = requireClient(client);
        String targetKey = requireKey(i18nKey);
        List<I18nMessageBo> rows = findRows(targetClient, targetKey);
        return rows.isEmpty() ? null : new I18nMessageVo(targetKey, toValues(rows));
    }

    @Transactional
    @Override
    public I18nMessageVo saveMessage(String client, I18nMessageDto dto) {
        String targetClient = requireClient(client);
        Preconditions.checkArgument(dto != null, messages.get("system.i18n.message.required"));
        String targetKey = requireKey(dto.getI18nKey());
        String previousKey = StringUtils.trimToNull(dto.getPreviousKey());
        if (previousKey != null) {
            previousKey = requireKey(previousKey);
        }
        List<I18nMessageValueDto> submittedValues = validateValues(dto.getValues());
        boolean migrating = previousKey != null && !previousKey.equals(targetKey);

        List<I18nMessageBo> sourceRows = findRows(targetClient, migrating ? previousKey : targetKey);
        if (migrating) {
            Preconditions.checkCondition(!sourceRows.isEmpty(),
                    messages.get("system.i18n.previousKey.notFound", previousKey));
            Preconditions.checkCondition(findRows(targetClient, targetKey).isEmpty(),
                    messages.get("system.i18n.targetKey.exists", targetKey));
        }

        validatePathConflict(targetClient, targetKey, migrating ? previousKey : null);
        Map<String, String> mergedValues = sourceRows.stream().collect(Collectors.toMap(
                I18nMessageBo::getLocale,
                I18nMessageBo::getI18nValue,
                (left, right) -> right,
                LinkedHashMap::new));
        applyValues(mergedValues, submittedValues);

        if (sourceRows.isEmpty() && mergedValues.isEmpty()) {
            Preconditions.checkArgument(false, messages.get("system.i18n.save.empty"));
        }

        if (migrating) {
            lambdaUpdate()
                    .eq(I18nMessageBo::getClient, targetClient)
                    .eq(I18nMessageBo::getI18nKey, previousKey)
                    .remove();
            saveNewRows(targetClient, targetKey, mergedValues);
        } else {
            persistRows(targetClient, targetKey, sourceRows, mergedValues);
        }
        return new I18nMessageVo(targetKey, toValues(mergedValues));
    }

    @Transactional
    @Override
    public void removeMessage(String client, String i18nKey) {
        String targetClient = requireClient(client);
        String targetKey = requireKey(i18nKey);
        lambdaUpdate()
                .eq(I18nMessageBo::getClient, targetClient)
                .eq(I18nMessageBo::getI18nKey, targetKey)
                .remove();
    }

    private String requireClient(String client) {
        String targetClient = StringUtils.trimToNull(client);
        Preconditions.checkArgument(targetClient != null, messages.get("system.i18n.client.required"));
        Preconditions.checkArgument(I18nMessageConstants.ADMIN_CLIENT.equals(targetClient),
                messages.get("system.i18n.client.unsupported", targetClient));
        return targetClient;
    }

    private String requireKey(String i18nKey) {
        String targetKey = StringUtils.trimToNull(i18nKey);
        Preconditions.checkArgument(targetKey != null, messages.get("system.i18n.key.required"));
        Preconditions.checkArgument(targetKey.length() <= MAX_KEY_LENGTH,
                messages.get("system.i18n.key.tooLong", MAX_KEY_LENGTH));
        Preconditions.checkArgument(KEY_PATTERN.matcher(targetKey).matches(),
                messages.get("system.i18n.key.invalid"));
        Preconditions.checkArgument(List.of(targetKey.split("\\.")).stream()
                        .noneMatch(FORBIDDEN_SEGMENTS::contains),
                messages.get("system.i18n.key.invalid"));
        return targetKey;
    }

    private String requireLocale(String locale) {
        Preconditions.checkArgument(SupportedLocale.supports(locale),
                messages.get("system.i18n.locale.unsupported", locale));
        return locale;
    }

    private List<I18nMessageValueDto> validateValues(List<I18nMessageValueDto> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> locales = new HashSet<>();
        for (I18nMessageValueDto value : values) {
            Preconditions.checkArgument(value != null, messages.get("system.i18n.value.required"));
            String locale = requireLocale(value.getLocale());
            Preconditions.checkArgument(locales.add(locale),
                    messages.get("system.i18n.locale.duplicate", locale));
            Preconditions.checkArgument(value.getValue() == null
                            || value.getValue().length() <= MAX_VALUE_LENGTH,
                    messages.get("system.i18n.value.tooLong", MAX_VALUE_LENGTH));
        }
        return values;
    }

    private void validatePathConflict(String client, String targetKey, String ignoredKey) {
        List<String> existingKeys = lambdaQuery()
                .select(I18nMessageBo::getI18nKey)
                .eq(I18nMessageBo::getClient, client)
                .list()
                .stream()
                .map(I18nMessageBo::getI18nKey)
                .distinct()
                .toList();
        String conflict = existingKeys.stream()
                .filter(key -> !key.equals(targetKey))
                .filter(key -> !key.equals(ignoredKey))
                .filter(key -> key.startsWith(targetKey + ".") || targetKey.startsWith(key + "."))
                .findFirst()
                .orElse(null);
        Preconditions.checkCondition(conflict == null,
                messages.get("system.i18n.key.pathConflict", targetKey, conflict));
    }

    private List<I18nMessageBo> findRows(String client, String i18nKey) {
        return lambdaQuery()
                .eq(I18nMessageBo::getClient, client)
                .eq(I18nMessageBo::getI18nKey, i18nKey)
                .list();
    }

    private void applyValues(Map<String, String> values, List<I18nMessageValueDto> submittedValues) {
        for (I18nMessageValueDto submitted : submittedValues) {
            if (StringUtils.isBlank(submitted.getValue())) {
                values.remove(submitted.getLocale());
            } else {
                values.put(submitted.getLocale(), submitted.getValue());
            }
        }
    }

    private void persistRows(
            String client,
            String i18nKey,
            List<I18nMessageBo> existingRows,
            Map<String, String> values) {
        Map<String, I18nMessageBo> existingByLocale = existingRows.stream().collect(Collectors.toMap(
                I18nMessageBo::getLocale,
                Function.identity()));
        for (I18nMessageBo existing : existingRows) {
            String value = values.get(existing.getLocale());
            if (value == null) {
                removeById(existing.getId());
            } else if (!Objects.equals(value, existing.getI18nValue())) {
                existing.setI18nValue(value);
                updateById(existing);
            }
        }
        values.entrySet().stream()
                .filter(entry -> !existingByLocale.containsKey(entry.getKey()))
                .map(entry -> newRow(client, i18nKey, entry.getKey(), entry.getValue()))
                .forEach(this::save);
    }

    private void saveNewRows(String client, String i18nKey, Map<String, String> values) {
        List<I18nMessageBo> rows = values.entrySet().stream()
                .map(entry -> newRow(client, i18nKey, entry.getKey(), entry.getValue()))
                .toList();
        if (!rows.isEmpty()) {
            saveBatch(rows);
        }
    }

    private I18nMessageBo newRow(String client, String i18nKey, String locale, String value) {
        I18nMessageBo row = new I18nMessageBo();
        row.setClient(client);
        row.setI18nKey(i18nKey);
        row.setLocale(locale);
        row.setI18nValue(value);
        return row;
    }

    private List<I18nMessageValueVo> toValues(Collection<I18nMessageBo> rows) {
        Map<String, String> values = rows.stream().collect(Collectors.toMap(
                I18nMessageBo::getLocale,
                I18nMessageBo::getI18nValue,
                (left, right) -> right));
        return toValues(values);
    }

    private List<I18nMessageValueVo> toValues(Map<String, String> values) {
        return SupportedLocale.codes().stream()
                .filter(values::containsKey)
                .map(locale -> new I18nMessageValueVo(locale, values.get(locale)))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void putPath(Map<String, Object> root, String i18nKey, String value) {
        String[] segments = i18nKey.split("\\.");
        Map<String, Object> cursor = root;
        for (int index = 0; index < segments.length - 1; index++) {
            cursor = (Map<String, Object>) cursor.computeIfAbsent(
                    segments[index], ignored -> new LinkedHashMap<>());
        }
        cursor.put(segments[segments.length - 1], value);
    }
}
