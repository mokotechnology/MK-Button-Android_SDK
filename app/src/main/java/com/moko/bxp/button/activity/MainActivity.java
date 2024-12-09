package com.moko.bxp.button.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.moko.bxp.button.R;
import com.moko.bxp.button.cr.activity.CRMainActivity;
import com.moko.bxp.button.cr.databinding.ActivityMainBinding;
import com.moko.bxp.button.d.activity.DMainActivity;
import com.moko.bxp.button.databinding.ActivityMainButtonBinding;
import com.moko.bxp.button.dialog.AlertMessageDialog;


public class MainActivity extends BaseActivity {

    private ActivityMainButtonBinding mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityMainButtonBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
    }

    public void onSelectCR(View view) {
        if (isWindowLocked()) return;
        startActivity(new Intent(this, CRMainActivity.class));
    }

    public void onSelectD(View view) {
        if (isWindowLocked()) return;
        startActivity(new Intent(this, DMainActivity.class));
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
