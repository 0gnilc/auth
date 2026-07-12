package com.gnilc.auth.authz.rbac.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BeanCopyUtilsTest {
    @Test
    void updatesOnlyNonNullProperties() {
        Mutable source = new Mutable(null, "new");
        Mutable target = new Mutable("keep", "old");

        BeanCopyUtils.copyNonNullProperties(source, target);

        assertThat(target.getFirst()).isEqualTo("keep");
        assertThat(target.getSecond()).isEqualTo("new");
    }

    public static final class Mutable {
        private String first;
        private String second;

        Mutable(String first, String second) {
            this.first = first;
            this.second = second;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(String second) {
            this.second = second;
        }
    }
}
