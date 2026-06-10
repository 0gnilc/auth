package com.gnilc.authz.web.annotation;

import com.gnilc.authz.annotation.EnableAuthorization;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableAuthorization
@Import({WebAuthorizationConfiguration.class})
@Documented
public @interface EnableWebAuthorization {

}
