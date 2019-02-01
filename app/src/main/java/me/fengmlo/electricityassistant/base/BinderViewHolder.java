package me.fengmlo.electricityassistant.base;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class BinderViewHolder<T> extends RecyclerView.ViewHolder {

    protected BinderClickListener<T> onItemClickListener;

    public BinderViewHolder(View itemView) {
        super(itemView);
    }

    public BinderClickListener<T> getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemClickListener(BinderClickListener<T> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
