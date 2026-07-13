package com.gnilc.common.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Arrays;

public final class BeanCopyUtils {

    public static void copyNonNullProperties(Object source, Object target) {
        BeanWrapper src = new BeanWrapperImpl(source);
        String[] nullProperties = Arrays.stream(src.getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .filter(name -> src.getPropertyValue(name) == null)
                .toArray(String[]::new);
        BeanUtils.copyProperties(source, target, nullProperties);
    }
}
