package com.hbm.interfaces;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Contended {

    /**
     * The (optional) contention group tag.
     * This tag is only meaningful for field level annotations.
     *
     * @return contention group tag.
     */
    String value() default "";
}

