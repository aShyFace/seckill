package com.hmdp.log;

import java.lang.annotation.*;

@Target(value = ElementType.TYPE)
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
public @interface LogApi {
}
