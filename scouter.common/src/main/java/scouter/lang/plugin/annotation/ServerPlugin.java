package scouter.lang.plugin.annotation;

import scouter.lang.plugin.PluginConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2016. 3. 19.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServerPlugin {
    String value() default PluginConstants.PLUGIN_SERVER_COUNTER;
}
