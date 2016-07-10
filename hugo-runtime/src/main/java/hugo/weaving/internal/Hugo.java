package hugo.weaving.internal;

import android.os.Build;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.concurrent.TimeUnit;

import hugo.weaving.DebugLog;

@Aspect
public class Hugo {
  private static volatile boolean enabled = true;

  @Pointcut("within(@hugo.weaving.DebugLog *)")
  public void withinAnnotatedClass() {}

  @Pointcut("execution(!synthetic * *(..)) && withinAnnotatedClass()")
  public void methodInsideAnnotatedType() {}

  @Pointcut("execution(!synthetic *.new(..)) && withinAnnotatedClass()")
  public void constructorInsideAnnotatedType() {}

  @Pointcut("execution(@hugo.weaving.DebugLog * *(..)) || methodInsideAnnotatedType()")
  public void method() {}

  @Pointcut("execution(@hugo.weaving.DebugLog *.new(..)) || constructorInsideAnnotatedType()")
  public void constructor() {}

  public static void setEnabled(boolean enabled) {
    Hugo.enabled = enabled;
  }

  @Around("method() || constructor()")
  public Object logAndExecute(ProceedingJoinPoint joinPoint) throws Throwable {

    enterMethod(joinPoint);


    long startNanos = System.nanoTime();
    Object result = joinPoint.proceed();
    long stopNanos = System.nanoTime();


    long lengthMillis = stopNanos - startNanos;
    exitMethod(joinPoint, result, lengthMillis);


    return result;
  }

  private static void enterMethod(JoinPoint joinPoint) {
    if (!enabled) return;

    CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();

    Class<?> cls = codeSignature.getDeclaringType();
    String methodName = codeSignature.getName();
    String[] parameterNames = codeSignature.getParameterNames();
    Object[] parameterValues = joinPoint.getArgs();
//    for(int i=0 ; i<10;i++){
//        System.out.println("dddddddddddddddddddddddddddddddddddddddddddddddd");
//    }
    StringBuilder builder = new StringBuilder("\u21E2 ");
    builder.append(methodName).append('(');
    for (int i = 0; i < parameterValues.length; i++) {

      if (i > 0 && i<10) {
        builder.append(", ");
      }
      builder.append(parameterNames[i]).append('=');
      builder.append(Strings.toString(parameterValues[i]));
      if(i==10){
        builder.append("...");
        break;
      }
    }
    builder.append(')');

    if (Looper.myLooper() != Looper.getMainLooper()) {
      builder.append(" [Thread:\"").append(Thread.currentThread().getName()).append("\"]");
    }

    Log.v(asTag(cls), builder.toString());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      final String section = builder.toString().substring(2);
      Trace.beginSection(section);
    }
  }


  private static void exitMethod(JoinPoint joinPoint, Object result, long tLenProcedd) {
    if (!enabled) return;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Trace.endSection();
    }

    Signature signature = joinPoint.getSignature();

    Class<?> cls = signature.getDeclaringType();

    String methodName = signature.getName();

    //设置三个配置的默认值，时间门限为0,单位默认为毫秒，默认不打印日志
    long timeThreshold = 0;
    String timeUnit = "mill";
    boolean isShowStack = false;
    //获取DebugLog注解对象
    DebugLog d1 = (DebugLog) signature.getDeclaringType().getAnnotation(DebugLog.class);
    //如果注解在方法上，通过MethodSignature获得
    if(d1==null){
      MethodSignature ms = (MethodSignature)signature;
      d1 = (DebugLog) ms.getMethod().getAnnotation(DebugLog.class);
    }
    //根据获取到的DebugLog对象，获得配置
    timeThreshold = d1.threshold();
    timeUnit = d1.timeUnit();
    isShowStack = d1.isShowStack();

    boolean hasReturnType = signature instanceof MethodSignature
        && ((MethodSignature) signature).getReturnType() != void.class;

    //根据时间单位换算tLenProceed的大小
    if(timeUnit .equals("mill")){
      tLenProcedd = TimeUnit.NANOSECONDS.toMillis(tLenProcedd);
      timeUnit = "ms";
    }
    else  if(timeUnit.equals("micro")){
      tLenProcedd = TimeUnit.NANOSECONDS.toMicros(tLenProcedd);
    }else if(timeUnit.equals("nano")){
      tLenProcedd = tLenProcedd;
    }else{
      tLenProcedd = TimeUnit.NANOSECONDS.toMillis(tLenProcedd);
      timeUnit = "ms";
    }

    //当运行时长大于门限时，输出运行时间
    if(tLenProcedd>=timeThreshold){
      StringBuilder builder = new StringBuilder("\u21E0 ")
              .append(methodName)
              .append(" [")
              .append(tLenProcedd).append("]")
              .append(timeUnit);

      if (hasReturnType) {
        builder.append(" = ");
        builder.append(Strings.toString(result));
      }
      Log.v(asTag(cls), builder.toString());

    }
    //小于门限时不输出运行情况
    else{
      Log.v("notime","lower than the threshold,This turn does not neet output");
    }
    //需要打印日志时，打印
    if(isShowStack){
      Exception e = new Exception("this is a log");
      e.printStackTrace();
    }
  }

  private static String asTag(Class<?> cls) {
    if (cls.isAnonymousClass()) {
      return asTag(cls.getEnclosingClass());
    }
    return cls.getSimpleName();
  }
}
