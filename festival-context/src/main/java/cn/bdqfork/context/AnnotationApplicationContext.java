package cn.bdqfork.context;

import cn.bdqfork.aop.processor.AopProxyProcessor;
import cn.bdqfork.context.factory.AnnotationBeanDefinitionReader;
import cn.bdqfork.core.exception.BeansException;
import cn.bdqfork.core.factory.AbstractBeanFactory;
import cn.bdqfork.core.factory.ConfigurableBeanFactory;
import cn.bdqfork.core.factory.DefaultBeanFactory;
import cn.bdqfork.core.factory.DefaultJSR250BeanFactory;
import cn.bdqfork.core.factory.definition.BeanDefinition;
import cn.bdqfork.core.factory.definition.BeanDefinitionBuilder;
import cn.bdqfork.core.factory.processor.BeanFactoryPostProcessor;
import cn.bdqfork.core.factory.processor.BeanPostProcessor;
import cn.bdqfork.core.factory.registry.BeanDefinitionRegistry;
import cn.bdqfork.value.reader.GenericResourceReader;
import cn.bdqfork.value.reader.ResourceReader;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author bdq
 * @since 2020/1/8
 */
@Slf4j
public class AnnotationApplicationContext extends AbstractApplicationContext {
    /**
     * 是否启用JSR250
     */
    protected static boolean JSR250 = true;
    /**
     * 是否启用AOP
     */
    protected static boolean AOP = true;
    /**
     * 委托工厂
     */
    private AbstractBeanFactory delegateBeanFactory;

    private AnnotationBeanDefinitionReader beanDefinitionReader;

    static {
        ClassLoader classLoader = AnnotationApplicationContext.class.getClassLoader();
        try {
            classLoader.loadClass("javax.annotation.Resource");
            log.info("enable jsr250 !");
        } catch (ClassNotFoundException e) {
            JSR250 = false;
        }
        try {
            classLoader.loadClass("cn.bdqfork.aop.factory.AopProxyBeanFactory");
            log.info("enable aop !");
        } catch (ClassNotFoundException e) {
            AOP = false;
        }
    }


    public AnnotationApplicationContext(String... scanPaths) throws BeansException {
        super(scanPaths);
        log.info("context is ready to use !");
    }

    @Override
    protected void createBeanFactory() {
        if (JSR250) {
            this.delegateBeanFactory = new DefaultJSR250BeanFactory();
        } else {
            this.delegateBeanFactory = new DefaultBeanFactory();
        }
        if (log.isTraceEnabled()) {
            log.trace("create BeanFactory of type {} !", this.delegateBeanFactory.getClass().getName());
        }
    }

    @Override
    protected void registerResourceReader() throws BeansException {
        if (log.isTraceEnabled()) {
            log.trace("register ResourceReader of type {} !", GenericResourceReader.class.getName());
        }

        BeanDefinition beanDefinition = new BeanDefinitionBuilder()
                .setBeanName("resourceReader")
                .setBeanClass(GenericResourceReader.class)
                .setScope(BeanDefinition.SINGLETON)
                .build();

        delegateBeanFactory.registerBeanDefinition(beanDefinition.getBeanName(), beanDefinition);

        ResourceReader resourceReader = delegateBeanFactory.getBean(beanDefinition.getBeanName());

        AnnotationBeanDefinitionReader annotationBeanDefinitionReader = new AnnotationBeanDefinitionReader(JSR250);

        annotationBeanDefinitionReader.setResourceReader(resourceReader);

        beanDefinitionReader = annotationBeanDefinitionReader;
    }

    @Override
    protected void registerProcessor() throws BeansException {
        if (AOP) {
            if (log.isTraceEnabled()) {
                log.trace("register aop processor !");
            }

            BeanDefinition beanDefinition = new BeanDefinitionBuilder()
                    .setBeanName("aopProcessor")
                    .setBeanClass(AopProxyProcessor.class)
                    .setScope(BeanDefinition.SINGLETON)
                    .build();
            this.delegateBeanFactory.registerBeanDefinition(beanDefinition.getBeanName(), beanDefinition);
        }
    }

    @Override
    public void scan(String... scanPaths) throws BeansException {
        for (String scanPath : scanPaths) {
            if (log.isTraceEnabled()) {
                log.trace("scan bean by path {} !", scanPath);
            }
            beanDefinitionReader.scan(scanPath);
        }
        refresh();
    }

    private void refresh() throws BeansException {
        registerBeanDefinition();
        registerBeanFactoryPostProcessor();
        registerBeanPostProcessor();
    }

    private void registerBeanDefinition() throws BeansException {
        if (log.isTraceEnabled()) {
            log.trace("register BeanDefinition !");
        }

        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionReader.getBeanDefinitions().entrySet()) {
            BeanDefinitionRegistry registry = delegateBeanFactory;
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (registry.containBeanDefinition(beanName)) {
                if (log.isTraceEnabled()) {
                    log.trace("duplicate bean of type {} will not be registered !", beanDefinition.getBeanClass().getName());
                }
                continue;
            }
            beanDefinitionReader.resolveInjectedPoint(beanDefinition, delegateBeanFactory);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerBeanPostProcessor() throws BeansException {
        if (log.isTraceEnabled()) {
            log.trace("register bean processor !");
        }

        for (BeanPostProcessor beanPostProcessor : delegateBeanFactory.getBeans(BeanPostProcessor.class).values()) {
            delegateBeanFactory.addPostBeanProcessor(beanPostProcessor);
        }
    }

    private void registerBeanFactoryPostProcessor() throws BeansException {
        if (log.isTraceEnabled()) {
            log.trace("register BeanFactory processor !");
        }

        for (BeanFactoryPostProcessor factoryPostProcessor : delegateBeanFactory.getBeans(BeanFactoryPostProcessor.class).values()) {
            factoryPostProcessor.postProcessBeanFactory(delegateBeanFactory);
        }

    }


    @Override
    public ConfigurableBeanFactory getConfigurableBeanFactory() {
        return delegateBeanFactory;
    }

    @Override
    public void close() {
        log.info("closing context !");
        delegateBeanFactory.destroySingletons();
        log.info("closed context !");
    }
}
