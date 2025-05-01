package com.swy.common.annotation;

import java.lang.annotation.*;

/**
 * 描述
 *
 * @author SkyWithYou
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {
    String value();
}
