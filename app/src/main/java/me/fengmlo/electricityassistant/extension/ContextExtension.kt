package me.fengmlo.electricityassistant.extension

import android.content.Context
import android.widget.Toast
import me.fengmlo.electricityassistant.App

fun Context.showToast(msg: String) {
    val toast = Toast.makeText(App.getContext(), "", Toast.LENGTH_SHORT)
    toast.setText(msg)
    toast.show()
}