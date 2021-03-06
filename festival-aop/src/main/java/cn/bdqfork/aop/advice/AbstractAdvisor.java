package cn.bdqfork.aop.advice;


import java.util.Objects;

/**
 * @author bdq
 * @since 2019/12/23
 */
public abstract class AbstractAdvisor implements Advisor {
    /**
     * 切点表达式，正则表达式
     */
    private String pointcut;
    /**
     * 通知
     */
    private Advice advice;

    @Override
    public void setPointcut(String pointcut) {
        this.pointcut = pointcut;
    }

    @Override
    public String getPointcut() {
        return pointcut;
    }

    @Override
    public void setAdvice(Advice advice) {
        this.advice = advice;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public boolean isAdviceTypeOf(Class<?> adviceType) {
        return adviceType.isInstance(advice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractAdvisor that = (AbstractAdvisor) o;
        return pointcut.equals(that.pointcut) &&
                advice.equals(that.advice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pointcut, advice);
    }
}
