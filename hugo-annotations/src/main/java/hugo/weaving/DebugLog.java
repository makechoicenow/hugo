package hugo.weaving;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Target({TYPE, METHOD, CONSTRUCTOR})
@Retention(CLASS)
public @interface DebugLog {
    //定义三个配置属性
    // threshold是运行时间门限，大于该门限是输出运行时间
    //timeUnit是时间单位，默认为毫秒，配置为“micro”为microseconds，配置为“nano”为nanoseconds,其他不知名配置默认为mill处理
    //isShowStack是输出call stack配置，默认为false,不输出，设置为true时输出
    public long threshold() default 0;
    public String timeUnit() default "mill";
    public boolean isShowStack() default false;
}
