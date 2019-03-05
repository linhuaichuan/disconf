package com.baidu.disconf.client.scan.inner.statically.strategy.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.baidu.disconf.client.common.annotations.DisconfActiveBackupService;
import com.baidu.disconf.client.common.annotations.DisconfFile;
import com.baidu.disconf.client.common.annotations.DisconfFileItem;
import com.baidu.disconf.client.common.annotations.DisconfItem;
import com.baidu.disconf.client.common.annotations.DisconfUpdateService;
import com.baidu.disconf.client.common.update.IDisconfUpdatePipeline;
import com.baidu.disconf.client.scan.inner.common.ScanVerify;
import com.baidu.disconf.client.scan.inner.statically.model.ScanStaticModel;
import com.baidu.disconf.client.scan.inner.statically.strategy.ScanStaticStrategy;
import com.baidu.disconf.client.support.registry.impl.SpringRegistry;

/**
 * @类名称 SpringScanStatic.java
 * @类描述 <pre>spring 静态扫描器</pre>
 * @作者  庄梦蝶殇 linhuaichuan@veredholdings.com
 * @创建时间 2019年3月5日 下午3:53:03
 * @版本 1.0.0
 *
 * @修改记录
 * <pre>
 *     版本                       修改人 		修改日期 		 修改内容描述
 *     ----------------------------------------------
 *     1.0.0 	       庄梦蝶殇 	2019年3月5日             
 *     ----------------------------------------------
 * </pre>
 */
public class SpringScanStatic implements ScanStaticStrategy {
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(SpringScanStatic.class);
    
    private ApplicationContext context;
    
    private DefaultListableBeanFactory factory;
    
    /**
     * 构造函数
     */
    public SpringScanStatic() {
        this.context = SpringRegistry.getApplicationContext();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ScanStaticModel scan(List<String> packNameList) {
        ScanStaticModel scanModel = new ScanStaticModel();
        Set<Class<?>> fileClasses = new HashSet<Class<?>>();
        Set<Class<?>> fileBackupClasses = new HashSet<Class<?>>();
        Set<Class<?>> fileUpdateClasses = new HashSet<Class<?>>();
        Set<Method> fileItemMethods = new HashSet<Method>();
        Set<Method> itemMethods = new HashSet<Method>();
        Class<IDisconfUpdatePipeline> pipeline = null;
        factory = (DefaultListableBeanFactory)context.getAutowireCapableBeanFactory();
        String[] beanNames = factory.getBeanDefinitionNames();
        for (String item : beanNames) {
            Class<?> itemClass = factory.getType(item);
            // 获取DisconfFile class
            if (null != factory.findAnnotationOnBean(item, DisconfFile.class)) {
                fileClasses.add(itemClass);
            }
            // 获取DisconfActiveBackupService
            if (null != factory.findAnnotationOnBean(item, DisconfActiveBackupService.class)) {
                fileBackupClasses.add(factory.getType(item));
            }
            // 获取DisconfUpdateService
            if (null != factory.findAnnotationOnBean(item, DisconfUpdateService.class)) {
                fileUpdateClasses.add(factory.getType(item));
            }
            Method[] fileMethods = itemClass.getDeclaredMethods();
            for (Method method : fileMethods) {
                // 获取DisconfFileItem method
                if (null != method.getAnnotation(DisconfFileItem.class)) {
                    fileItemMethods.add(method);
                }
                // 获取DisconfItem method
                if (null != method.getAnnotation(DisconfItem.class)) {
                    itemMethods.add(method);
                }
            }
            if (null == pipeline && itemClass.isAssignableFrom(IDisconfUpdatePipeline.class)) {
                pipeline = (Class<IDisconfUpdatePipeline>)factory.getBean(item);
            }
        }
        scanModel.setDisconfFileClassSet(fileClasses);
        scanModel.setDisconfFileItemMethodSet(fileItemMethods);
        scanModel.setDisconfItemMethodSet(itemMethods);
        scanModel.setDisconfActiveBackupServiceClassSet(fileBackupClasses);
        scanModel.setDisconfUpdateService(fileUpdateClasses);
        // update pipeline
        if (null != pipeline) {
            scanModel.setiDisconfUpdatePipeline(pipeline);
        }
        // 分析出配置文件MAP
        analysis4DisconfFile(scanModel);
        return scanModel;
    }
    
    /**
     * 分析出配置文件与配置文件中的Field的Method的MAP
     */
    private void analysis4DisconfFile(ScanStaticModel scanModel) {
        
        Map<Class<?>, Set<Method>> disconfFileItemMap = new HashMap<Class<?>, Set<Method>>();
        
        //
        // 配置文件MAP
        //
        Set<Class<?>> classdata = scanModel.getDisconfFileClassSet();
        for (Class<?> classFile : classdata) {
            disconfFileItemMap.put(classFile, new HashSet<Method>());
        }
        
        //
        // 将配置文件与配置文件中的配置项进行关联
        //
        Set<Method> af1 = scanModel.getDisconfFileItemMethodSet();
        for (Method method : af1) {
            
            Class<?> thisClass = method.getDeclaringClass();
            
            if (disconfFileItemMap.containsKey(thisClass)) {
                Set<Method> fieldSet = disconfFileItemMap.get(thisClass);
                fieldSet.add(method);
                disconfFileItemMap.put(thisClass, fieldSet);
                
            } else {
                
                LOGGER.error("cannot find CLASS ANNOTATION " + DisconfFile.class.getName() + " for disconf file item: " + method.toString());
            }
        }
        
        //
        // 最后的校验
        //
        Iterator<Class<?>> iterator = disconfFileItemMap.keySet().iterator();
        while (iterator.hasNext()) {
            
            Class<?> classFile = iterator.next();
            
            // 校验是否所有配置文件都含有配置
            if (disconfFileItemMap.get(classFile).isEmpty()) {
                LOGGER.info("disconf file hasn't any items: " + classFile.getName());
                continue;
            }
            
            // 校验配置文件类型是否合适(目前只支持.properties类型)
            DisconfFile disconfFile = classFile.getAnnotation(DisconfFile.class);
            boolean fileTypeRight = ScanVerify.isDisconfFileTypeRight(disconfFile);
            if (!fileTypeRight) {
                LOGGER.warn("now do not support this file type" + disconfFile.toString());
                continue;
            }
        }
        
        // 设置
        scanModel.setDisconfFileItemMap(disconfFileItemMap);
    }
}
