package com.moko.bxp.button.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.moko.bxp.button.BaseApplication;
import com.moko.bxp.button.R;
import com.moko.bxp.button.utils.ToastUtils;
import com.moko.bxp.button.utils.Utils;

import java.io.File;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;


public class AboutActivity extends BaseActivity {
    @BindView(R.id.app_version)
    TextView appVersion;
    @BindView(R.id.tv_feedback_log)
    TextView tvFeedbackLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
    }


    public void onBack(View view) {
        finish();
    }

    public void onCompanyWebsite(View view) {
        if (isWindowLocked())
            return;
        Uri uri = Uri.parse("https://" + getString(R.string.company_website));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void onFeedback(View view) {
        if (isWindowLocked())
            return;
        File trackerLog = new File(BaseApplication.PATH_LOGCAT + File.separator + "MKButton.txt");
        File trackerLogBak = new File(BaseApplication.PATH_LOGCAT + File.separator + "MKButton.txt.bak");
        File trackerCrashLog = new File(BaseApplication.PATH_LOGCAT + File.separator + "crash_log.txt");
        if (!trackerLog.exists() || !trackerLog.canRead()) {
            ToastUtils.showToast(this, "File is not exists!");
            return;
        }
        String address = "Development@mokotechnology.com";
        StringBuilder mailContent = new StringBuilder("MKButton_");
        Calendar calendar = Calendar.getInstance();
        String date = Utils.calendar2strDate(calendar, "yyyyMMdd");
        mailContent.append(date);
        String title = mailContent.toString();
        if ((!trackerLogBak.exists() || !trackerLogBak.canRead())
                && (!trackerCrashLog.exists() || !trackerCrashLog.canRead())) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog);
        } else if (!trackerCrashLog.exists() || !trackerCrashLog.canRead()) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerLogBak);
        } else if (!trackerLogBak.exists() || !trackerLogBak.canRead()) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerCrashLog);
        } else {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerLogBak, trackerCrashLog);
        }
    }
}
