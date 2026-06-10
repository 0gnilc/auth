package com.gnilc.authz.rbac.annotation;

import com.gnilc.authz.annotation.EnableAuthorization;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableAuthorization
@Import({WebRbacAuthorizationConfiguration.class})
@Documented
public @interface EnableWebRbacAuthorization {

}
