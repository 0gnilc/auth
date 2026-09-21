package com.gnilc.bootstrap.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class I18nMessageBundleTest {

    private static final List<String> BASENAMES = List.of(
            "i18n/common/messages",
            "i18n/rbac/messages",
            "i18n/system/messages");
    private static final List<String> LOCALES = List.of("en_US", "ha_NG", "yo_NG", "zh_CN");
    private static final Pattern MESSAGE_ARGUMENT = Pattern.compile("\\{\\d+}");

    @Test
    void everyBackendBundleHasMatchingLocalesAndUniqueOwnership() throws Exception {
        Map<String, String> owners = new HashMap<>();

        for (String basename : BASENAMES) {
            Set<String> defaults = keys(basename + ".properties");
            for (String locale : LOCALES) {
                assertThat(keys(basename + "_" + locale + ".properties")).isEqualTo(defaults);
            }
            for (String key : defaults) {
                assertThat(owners.putIfAbsent(key, basename))
                        .as("message key %s must have one owning module", key)
                        .isNull();
            }
        }
    }

    @Test
    void everyBackendBundleDefinesEachMessageKeyOnce() throws Exception {
        for (String basename : BASENAMES) {
            assertUniqueKeys(basename + ".properties");
            for (String locale : LOCALES) {
                assertUniqueKeys(basename + "_" + locale + ".properties");
            }
        }
    }

    @Test
    void everyBackendBundleUsesMatchingMessageArguments() throws Exception {
        for (String basename : BASENAMES) {
            Properties defaults = messages(basename + ".properties");
            for (String locale : LOCALES) {
                Properties localized = messages(
                        basename + "_" + locale + ".properties");
                for (String key : defaults.stringPropertyNames()) {
                    assertThat(arguments(localized.getProperty(key)))
                            .as("message %s in %s must use the default arguments", key, locale)
                            .isEqualTo(arguments(defaults.getProperty(key)));
                }
            }
        }
    }

    private Set<String> keys(String path) throws Exception {
        return messages(path).stringPropertyNames();
    }

    private Properties messages(String path) throws Exception {
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private Set<String> arguments(String message) {
        Set<String> arguments = new HashSet<>();
        Matcher matcher = MESSAGE_ARGUMENT.matcher(message);
        while (matcher.find()) {
            arguments.add(matcher.group());
        }
        return arguments;
    }

    private void assertUniqueKeys(String path) throws Exception {
        Set<String> keys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                int separator = line.indexOf('=');
                if (separator <= 0 || line.startsWith("#") || line.startsWith("!")) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                if (!keys.add(key)) {
                    duplicates.add(key);
                }
            }
        }
        assertThat(duplicates)
                .as("message resource %s must not define duplicate keys", path)
                .isEmpty();
    }
}
