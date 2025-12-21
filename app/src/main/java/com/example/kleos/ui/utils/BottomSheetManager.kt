package com.example.kleos.ui.utils

import com.google.android.material.bottomsheet.BottomSheetDialog

object BottomSheetManager {
    private var currentDialog: BottomSheetDialog? = null
    
    fun showDialog(dialog: BottomSheetDialog) {
        // Закрываем предыдущее окно, если оно открыто
        currentDialog?.dismiss()
        
        // Сохраняем ссылку на новое окно
        currentDialog = dialog
        
        // Очищаем ссылку при закрытии диалога
        dialog.setOnDismissListener {
            if (currentDialog == dialog) {
                currentDialog = null
            }
        }
        
        dialog.show()
    }
    
    fun dismissCurrent() {
        currentDialog?.dismiss()
        currentDialog = null
    }
    
    fun isShowing(): Boolean {
        return currentDialog?.isShowing == true
    }
    
    fun getCurrentDialog(): BottomSheetDialog? = currentDialog
}

