package me.fengmlo.electricityassistant.util;

import android.widget.Toast;

import me.fengmlo.electricityassistant.App;

public class ToastUtil {

    public static void show(String message) {
        Toast toast = Toast.makeText(App.getContext(), "", Toast.LENGTH_SHORT);
        toast.setText(message);
        toast.show();
    }
}
