package play.modules.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Delete {

    String value();

    String params() default "";

    int priority() default -1;

    String consume() default "html'";

    String produce() default "*'";

    String accept() default "html'";

}



