package com.gnilc.authz.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({AuthorizationConfiguration.class})
@Documented
public @interface EnableAuthorization {

}
