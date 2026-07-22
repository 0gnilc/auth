package com.gnilc.system.i18n.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.i18n.SupportedLocale;
import com.gnilc.common.utils.PageResult;
import com.gnilc.system.i18n.I18nConstants;
import com.gnilc.system.i18n.dao.I18nDao;
import com.gnilc.system.i18n.entity.bo.I18nBo;
import com.gnilc.system.i18n.entity.dto.I18nValueDto;
import com.gnilc.system.i18n.entity.dto.I18nPageDto;
import com.gnilc.system.i18n.entity.dto.I18nDto;
import com.gnilc.system.i18n.entity.vo.I18nValueVo;
import com.gnilc.system.i18n.entity.vo.I18nValuesVo;
import com.gnilc.system.i18n.entity.vo.I18nItemVo;
import com.gnilc.system.i18n.service.I18nService;
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
public class I18nServiceImpl extends ServiceImpl<I18nDao, I18nBo> implements I18nService {

    private static final int MAX_KEY_LENGTH = 191;
    private static final int MAX_VALUE_LENGTH = 4000;
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*$");
    private static final Set<String> FORBIDDEN_SEGMENTS = Set.of(
            "__proto__", "prototype", "constructor");

    private final I18nMessageService messages;

    public I18nServiceImpl(I18nMessageService messages) {
        this.messages = messages;
    }

    @Override
    public Map<String, Object> getBundle(String client) {
        String targetClient = requireClient(client);
        List<I18nBo> rows = lambdaQuery()
                .eq(I18nBo::getClient, targetClient)
                .orderByAsc(I18nBo::getI18nKey)
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
    public PageResult<I18nItemVo> getPage(String client, I18nPageDto dto) {
        String targetClient = requireClient(client);
        I18nPageDto query = dto == null ? new I18nPageDto() : dto;
        if (StringUtils.isNotBlank(query.getClient())) {
            Preconditions.checkArgument(targetClient.equals(query.getClient()),
                    messages.get("system.i18n.client.mismatch"));
        }
        if (StringUtils.isNotBlank(query.getLocale())) {
            requireLocale(query.getLocale());
        }

        QueryWrapper<I18nBo> keyQuery = new QueryWrapper<>();
        keyQuery.select("client", "i18n_key")
                .eq("client", targetClient)
                .like(StringUtils.isNotBlank(query.getKey()), "i18n_key", query.getKey())
                .like(StringUtils.isNotBlank(query.getValue()), "i18n_value", query.getValue())
                .eq(StringUtils.isNotBlank(query.getLocale()), "locale", query.getLocale())
                .groupBy("client", "i18n_key")
                .orderByAsc("i18n_key");
        IPage<I18nBo> keyPage = page(query.getPage(), keyQuery);
        List<String> keys = keyPage.getRecords().stream().map(I18nBo::getI18nKey).toList();
        if (keys.isEmpty()) {
            return PageResult.of(keyPage, List.of());
        }

        Map<String, List<I18nBo>> rowsByKey = lambdaQuery()
                .eq(I18nBo::getClient, targetClient)
                .in(I18nBo::getI18nKey, keys)
                .list()
                .stream()
                .collect(Collectors.groupingBy(I18nBo::getI18nKey));
        List<I18nItemVo> items = keys.stream()
                .map(key -> new I18nItemVo(
                        targetClient,
                        key,
                        toValues(rowsByKey.getOrDefault(key, List.of()))))
                .toList();
        return PageResult.of(keyPage, items);
    }

    @Override
    public I18nValuesVo getValues(String client, String i18nKey) {
        String targetClient = requireClient(client);
        String targetKey = requireKey(i18nKey);
        List<I18nBo> rows = findRows(targetClient, targetKey);
        return rows.isEmpty() ? null : new I18nValuesVo(targetKey, toValues(rows));
    }

    @Transactional
    @Override
    public I18nValuesVo saveMessage(String client, I18nDto dto) {
        String targetClient = requireClient(client);
        Preconditions.checkArgument(dto != null, messages.get("system.i18n.message.required"));
        String targetKey = requireKey(dto.getI18nKey());
        String previousKey = StringUtils.trimToNull(dto.getPreviousKey());
        if (previousKey != null) {
            previousKey = requireKey(previousKey);
        }
        List<I18nValueDto> submittedValues = validateValues(dto.getValues());
        boolean migrating = previousKey != null && !previousKey.equals(targetKey);

        List<I18nBo> sourceRows = findRows(targetClient, migrating ? previousKey : targetKey);
        if (migrating) {
            Preconditions.checkCondition(!sourceRows.isEmpty(),
                    messages.get("system.i18n.previousKey.notFound", previousKey));
            Preconditions.checkCondition(findRows(targetClient, targetKey).isEmpty(),
                    messages.get("system.i18n.targetKey.exists", targetKey));
        }

        validatePathConflict(targetClient, targetKey, migrating ? previousKey : null);
        Map<String, String> mergedValues = sourceRows.stream().collect(Collectors.toMap(
                I18nBo::getLocale,
                I18nBo::getI18nValue,
                (left, right) -> right,
                LinkedHashMap::new));
        applyValues(mergedValues, submittedValues);

        if (sourceRows.isEmpty() && mergedValues.isEmpty()) {
            Preconditions.checkArgument(false, messages.get("system.i18n.save.empty"));
        }

        if (migrating) {
            lambdaUpdate()
                    .eq(I18nBo::getClient, targetClient)
                    .eq(I18nBo::getI18nKey, previousKey)
                    .remove();
            saveNewRows(targetClient, targetKey, mergedValues);
        } else {
            persistRows(targetClient, targetKey, sourceRows, mergedValues);
        }
        return new I18nValuesVo(targetKey, toValues(mergedValues));
    }

    @Transactional
    @Override
    public void removeMessage(String client, String i18nKey) {
        String targetClient = requireClient(client);
        String targetKey = requireKey(i18nKey);
        lambdaUpdate()
                .eq(I18nBo::getClient, targetClient)
                .eq(I18nBo::getI18nKey, targetKey)
                .remove();
    }

    private String requireClient(String client) {
        String targetClient = StringUtils.trimToNull(client);
        Preconditions.checkArgument(targetClient != null, messages.get("system.i18n.client.required"));
        Preconditions.checkArgument(I18nConstants.ADMIN_CLIENT.equals(targetClient),
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

    private List<I18nValueDto> validateValues(List<I18nValueDto> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> locales = new HashSet<>();
        for (I18nValueDto value : values) {
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
                .select(I18nBo::getI18nKey)
                .eq(I18nBo::getClient, client)
                .list()
                .stream()
                .map(I18nBo::getI18nKey)
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

    private List<I18nBo> findRows(String client, String i18nKey) {
        return lambdaQuery()
                .eq(I18nBo::getClient, client)
                .eq(I18nBo::getI18nKey, i18nKey)
                .list();
    }

    private void applyValues(Map<String, String> values, List<I18nValueDto> submittedValues) {
        for (I18nValueDto submitted : submittedValues) {
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
            List<I18nBo> existingRows,
            Map<String, String> values) {
        Map<String, I18nBo> existingByLocale = existingRows.stream().collect(Collectors.toMap(
                I18nBo::getLocale,
                Function.identity()));
        for (I18nBo existing : existingRows) {
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
        List<I18nBo> rows = values.entrySet().stream()
                .map(entry -> newRow(client, i18nKey, entry.getKey(), entry.getValue()))
                .toList();
        if (!rows.isEmpty()) {
            saveBatch(rows);
        }
    }

    private I18nBo newRow(String client, String i18nKey, String locale, String value) {
        I18nBo row = new I18nBo();
        row.setClient(client);
        row.setI18nKey(i18nKey);
        row.setLocale(locale);
        row.setI18nValue(value);
        return row;
    }

    private List<I18nValueVo> toValues(Collection<I18nBo> rows) {
        Map<String, String> values = rows.stream().collect(Collectors.toMap(
                I18nBo::getLocale,
                I18nBo::getI18nValue,
                (left, right) -> right));
        return toValues(values);
    }

    private List<I18nValueVo> toValues(Map<String, String> values) {
        return SupportedLocale.codes().stream()
                .filter(values::containsKey)
                .map(locale -> new I18nValueVo(locale, values.get(locale)))
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
