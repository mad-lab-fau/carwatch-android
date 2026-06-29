package de.fau.cs.mad.carwatch.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import de.fau.cs.mad.carwatch.R;

public class AlertActivity extends AppCompatActivity {

    private static final String TAG = AlertActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //hide activity title
        setContentView(R.layout.activity_alert);

        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_warning_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        new CarwatchDialogBuilder(this)
                .setTitle(getString(R.string.warning_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.warning_already_taken_wakeup))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> finish())
                .show();
    }
}
