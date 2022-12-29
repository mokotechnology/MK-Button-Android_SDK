package com.moko.bxp.button.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.moko.bxp.button.R;
import com.moko.bxp.button.cr.activity.CRMainActivity;
import com.moko.bxp.button.dialog.AlertMessageDialog;


public class MainActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_button);
    }

    public void onSelectCR(View view) {
        if (isWindowLocked()) return;
        startActivity(new Intent(this, CRMainActivity.class));
    }

    public void onSelectD(View view) {
        if (isWindowLocked()) return;
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage(R.string.main_exit_tips);
        dialog.setOnAlertConfirmListener(() -> MainActivity.this.finish());
        dialog.show(getSupportFragmentManager());
    }
}
