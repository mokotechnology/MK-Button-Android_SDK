package com.moko.bxp.button.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.bxp.button.R;
import com.moko.support.entity.LightSensorStoreData;

public class LightSensorDataListAdapter extends BaseQuickAdapter<LightSensorStoreData, BaseViewHolder> {
    public LightSensorDataListAdapter() {
        super(R.layout.item_export_data_light);
    }

    @Override
    protected void convert(BaseViewHolder helper, LightSensorStoreData item) {
        helper.setText(R.id.tv_time, item.time);
        helper.setText(R.id.tv_light_sensor_status, item.status);
    }
}
