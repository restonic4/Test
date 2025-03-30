package com.chaotic_loom.game.registries.components;

import com.chaotic_loom.game.core.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Registration {
    Environment environment();
    int priority() default 0;
}