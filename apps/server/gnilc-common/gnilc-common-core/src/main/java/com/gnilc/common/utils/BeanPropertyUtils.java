package com.gnilc.common.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JavaBean 属性工具。
 */
public final class BeanPropertyUtils {

    private BeanPropertyUtils() {
    }

    /**
     * 将源对象中的非 {@code null} 属性复制到目标对象。
     */
    public static void copyNonNullProperties(Object source, Object target) {
        BeanWrapper src = new BeanWrapperImpl(source);
        String[] nullProperties = Arrays.stream(src.getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .filter(name -> src.getPropertyValue(name) == null)
                .toArray(String[]::new);
        BeanUtils.copyProperties(source, target, nullProperties);
    }

    /**
     * 去除对象中可读写字符串属性的首尾空白，并将空白字符串转换为 {@code null}。
     *
     * @param target             待修改的对象
     * @param excludedProperties 不参与处理的属性名
     */
    public static void trimToNull(Object target, String... excludedProperties) {
        Objects.requireNonNull(target, "target");
        Set<String> exclusions = Arrays.stream(
                        excludedProperties == null ? new String[0] : excludedProperties)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        BeanWrapper wrapper = new BeanWrapperImpl(target);

        Arrays.stream(wrapper.getPropertyDescriptors())
                .filter(property -> property.getPropertyType() == String.class)
                .map(PropertyDescriptor::getName)
                .filter(property -> !exclusions.contains(property))
                .filter(wrapper::isReadableProperty)
                .filter(wrapper::isWritableProperty)
                .forEach(property -> {
                    String value = (String) wrapper.getPropertyValue(property);
                    if (value != null) {
                        String trimmed = value.trim();
                        wrapper.setPropertyValue(property, trimmed.isEmpty() ? null : trimmed);
                    }
                });
    }
}
