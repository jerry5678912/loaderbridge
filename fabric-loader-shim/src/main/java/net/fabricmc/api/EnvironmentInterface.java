package net.fabricmc.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Repeatable(EnvironmentInterfaces.class)
@Target(ElementType.TYPE)
@Documented
public @interface EnvironmentInterface {
    EnvType value();
    Class<?> itf();
}
