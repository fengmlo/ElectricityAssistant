package me.fengmlo.electricityassistant.base;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

public abstract class BaseActivity extends AppCompatActivity {

    protected SparseArray<OnActivityResultListener> onActivityResultListenerMap = new SparseArray<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 点击空白位置 隐藏软键盘
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return hideInput() || super.onTouchEvent(event);
    }

    protected boolean hideInput() {
        if (getCurrentFocus() != null) {
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        OnActivityResultListener listener;
        if ((listener = onActivityResultListenerMap.get(requestCode)) != null) {
            listener.OnActivityResult(resultCode, data);
            onActivityResultListenerMap.remove(requestCode);
        }
    }

    public void startActivityForResult(Intent intent, int requestCode, OnActivityResultListener listener) {
        onActivityResultListenerMap.put(requestCode, listener);
        startActivityForResult(intent, requestCode);
    }

    protected AppCompatActivity getContext() {
        return this;
    }

    protected abstract int getLayoutId();

    protected abstract void initView();
}
