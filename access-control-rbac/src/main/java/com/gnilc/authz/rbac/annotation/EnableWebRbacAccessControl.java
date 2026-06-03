package com.gnilc.authz.rbac.annotation;

import com.gnilc.authz.annotation.EnableAccessControl;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableAccessControl
@Import({WebRbacAccessControlConfiguration.class})
@Documented
public @interface EnableWebRbacAccessControl {

}
