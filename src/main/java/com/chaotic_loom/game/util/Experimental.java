package com.chaotic_loom.game.util;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.MetadataDefinition;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@MetadataDefinition
@Label("Experimental")
@Description("Element is not to be shown to a user by default")
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Experimental {
}
