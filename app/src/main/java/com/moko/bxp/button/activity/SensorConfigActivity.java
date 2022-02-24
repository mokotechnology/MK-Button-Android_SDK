package com.moko.bxp.button.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.moko.bxp.button.AppConstants;
import com.moko.bxp.button.R;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SensorConfigActivity extends BaseActivity {


    @BindView(R.id.rl_acceleration_sensor)
    RelativeLayout rlAccelerationSensor;
    @BindView(R.id.rl_th_sensor)
    RelativeLayout rlThSensor;
    @BindView(R.id.rl_light_sensor)
    RelativeLayout rlLightSensor;
    private int mDeviceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_config);
        ButterKnife.bind(this);
        mDeviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_DEVICE_TYPE, 0);
        rlAccelerationSensor.setVisibility((mDeviceType & 1) == 1 ? View.VISIBLE : View.GONE);
        rlThSensor.setVisibility((mDeviceType & 2) == 2 ? View.VISIBLE : View.GONE);
        rlLightSensor.setVisibility((mDeviceType & 4) == 4 ? View.VISIBLE : View.GONE);
    }


    public void onBack(View view) {
        finish();
    }

    public void onAccelerationSensor(View view) {
        if (isWindowLocked())
            return;
        startActivity(new Intent(this, AxisDataActivity.class));
    }

    public void onTHSensor(View view) {
        if (isWindowLocked())
            return;
        startActivity(new Intent(this, THDataActivity.class));
    }

    public void onLightSensor(View view) {
        if (isWindowLocked())
            return;
        startActivity(new Intent(this, LightSensorDataActivity.class));
    }
}
