package com.lyd.absolverdatabase.ui.views

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class BaseDialogBuilder constructor(
    context: Context // 注意，这里要传Activity.this，传别的context报错，说找不到主题
) : MaterialAlertDialogBuilder(context) {
    override fun create(): AlertDialog {
        return super.create().also { dialog ->
            dialog.window?.let {
                it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                if (Build.VERSION.SDK_INT >= 31){
                    it.attributes.blurBehindRadius = 32
                }
            }
        }
    }
}