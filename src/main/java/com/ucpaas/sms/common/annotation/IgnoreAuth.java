package com.ucpaas.sms.common.annotation;

import java.lang.annotation.*;

/**
 * 忽略拦截
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreAuth {

}
