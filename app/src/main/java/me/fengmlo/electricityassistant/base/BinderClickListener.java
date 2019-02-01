package me.fengmlo.electricityassistant.base;

import android.view.View;

public abstract class BinderClickListener<T> implements View.OnClickListener {

    private T bean;

    public T getBean() {
        return bean;
    }

    public void setBean(T bean) {
        this.bean = bean;
    }

    public static <T> BinderClickListener<T> getInstance(Operation<T> op) {
        return new BinderClickListener<T>() {
            @Override
            public void onClick(View v) {
                T bean = getBean();
                if (bean != null) {
                    op.run(bean);
                }
            }
        };
    }

    public interface Operation<T> {
        void run(T bean);
    }
}
