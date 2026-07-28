package com.gnilc.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BeanPropertyUtilsTest {

    @Test
    void copyNonNullPropertiesUpdatesOnlyNonNullProperties() {
        Mutable source = new Mutable(null, "new", null);
        Mutable target = new Mutable("keep", "old", 10);

        BeanPropertyUtils.copyNonNullProperties(source, target);

        assertThat(target.getFirst()).isEqualTo("keep");
        assertThat(target.getSecond()).isEqualTo("new");
        assertThat(target.getOrder()).isEqualTo(10);
    }

    @Test
    void trimToNullNormalizesWritableStringPropertiesOnly() {
        Mutable target = new Mutable("  Export  ", "   ", 10);

        BeanPropertyUtils.trimToNull(target);

        assertThat(target.getFirst()).isEqualTo("Export");
        assertThat(target.getSecond()).isNull();
        assertThat(target.getOrder()).isEqualTo(10);
    }

    @Test
    void trimToNullLeavesExcludedStringPropertiesUnchanged() {
        Mutable target = new Mutable("  Export  ", "   ", null);

        BeanPropertyUtils.trimToNull(target, "second");

        assertThat(target.getFirst()).isEqualTo("Export");
        assertThat(target.getSecond()).isEqualTo("   ");
    }

    public static final class Mutable {
        private String first;
        private Integer order;
        private String second;

        Mutable(String first, String second, Integer order) {
            this.first = first;
            this.second = second;
            this.order = order;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(String second) {
            this.second = second;
        }
    }
}
