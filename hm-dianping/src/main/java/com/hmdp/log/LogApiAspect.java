package com.hmdp.log;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
public class LogApiAspect {
	//切面点为标记了@LogApi注解的方法
  //  @Pointcut("@annotation(com.hmdp.log.LogApi)")
    // @Pointcut("execution(* com.example.controller.*.*(..))")
    @Pointcut("@within(com.hmdp.log.LogApi)") //所有添加了@LogApi的方法
    public void logApi() {
    }

	//环绕通知
    @Around("logApi()")
    @SuppressWarnings("unchecked")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long starTime = System.currentTimeMillis();
        //通过反射获取被调用方法的Class
        Class type = joinPoint.getSignature().getDeclaringType();
        //获取类名
        String typeName = type.getSimpleName();
        //获取日志记录对象Logger
        Logger logger = LoggerFactory.getLogger(type);
        //方法名
        String methodName = joinPoint.getSignature().getName();
        //获取参数列表
        Object[] args = joinPoint.getArgs();
        // 参数Class的数组
        Class<?> clazz[] = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            clazz[i] = args[i].getClass();
        }
        //Class<?> clazz[] = new Class[args.length];
        //for (int i = 0; i < args.length; i++) {
        //    if (args[i] instanceof Integer) {
        //        clazz[i] = Integer.TYPE;
        //    } else if (args[i] instanceof String) {
        //        clazz[i] = String.class;
        //    }
        //    // you can do additional checks for other data types if you want.
        //}
        //通过反射获取调用的方法method
        Method method = type.getMethod(methodName, clazz);
        //获取方法的参数
        Parameter[] parameters = method.getParameters();
        //拼接字符串，格式为{参数1:值1,参数2::值2}
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.getName();
            sb.append(name).append(":").append(args[i]).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.lastIndexOf(","));
        }
        //执行结果
        Object res;
        try {
            //执行目标方法，获取执行结果
            res = joinPoint.proceed();
            logger.debug("调用{}.{}方法成功，参数为[{}]，返回结果[{}]", typeName, methodName, sb.toString(), JSONObject.toJSONString(res));
        } catch (Exception e) {
            logger.error("{}.{}方法发生异常", typeName, methodName);
            //如果发生异常，则抛出异常
            throw e;
        } finally {
            logger.debug("{}.{}方法，耗时{}ms", typeName, methodName, (System.currentTimeMillis() - starTime));
        }
        //返回执行结果
        return res;
    }
}
