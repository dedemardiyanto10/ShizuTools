package com.toolbox.shizutools;

import android.os.Build;
import android.view.WindowManager;
import androidx.appcompat.app.AlertDialog;

public class DialogHelper {
    public static void applyVisuals(AlertDialog dialog, int blurRadius, float dimAmount) {
        if (dialog.getWindow() == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            dialog.getWindow().getAttributes().setBlurBehindRadius(blurRadius);
        }

        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setDimAmount(dimAmount);
    }

    public static void applyDefaultStyle(AlertDialog dialog) {
        applyVisuals(dialog, 25, 0.5f);
    }
}
