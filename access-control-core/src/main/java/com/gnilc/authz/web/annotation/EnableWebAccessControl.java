package com.gnilc.authz.web.annotation;

import com.gnilc.authz.annotation.EnableAccessControl;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableAccessControl
@Import({AccessControlWebConfiguration.class})
@Documented
public @interface EnableWebAccessControl {

}
