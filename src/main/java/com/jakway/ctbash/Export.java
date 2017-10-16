package com.jakway.ctbash;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
//only fields can be exported
@Target(ElementType.FIELD)
public @interface Export {
    String exportAs() default "";
}