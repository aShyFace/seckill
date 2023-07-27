package com.hmdp.utils;

import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BeanCopyUtils {
    private BeanCopyUtils() {};

    /*
    从左到右地说下这三个T吧：
        - 第1个T（<T>）：告诉java这是一个泛型方法，并且泛型标记符为T
        - 第2个T（T）：  方法的返回值类型为 泛型T
        - 第3个T（<T>）：只能传入T类型的字节码对象，不写代表 可传入任何类的字节码对象（支持泛型的类，idea在提示的时候，会显示“<>”）
    */
    public static <T> T copyBean(Object src, Class<T> clazz) {
//        newInstance()调用的是类的无参构造方法，所以用之前得确定这个类有无参构造
        T result = null;
        try {
            result = clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(0);
        }
        BeanUtils.copyProperties(src, result);
        return result;
    }

    public static <O,E> List<E> copyBeanList(List<O> list, Class<E> clazz){
        List<E> collect = list.stream()
                .map(o -> copyBean(o, clazz))
                .collect(Collectors.toList());
        return collect;
    }


    //public static Map<String, String> objectToMap(Object object){
    //    Map<String, String> dataMap = new HashMap<>();
    //    Class<?> clazz = object.getClass();
    //    for (Field field : clazz.getDeclaredFields()) {
    //        try {
    //            field.setAccessible(true);
    //            dataMap.put(field.getName(),field.get(object));
    //        } catch (IllegalAccessException e) {
    //            e.printStackTrace();
    //        }
    //    }
    //    return dataMap;
    //}
}
