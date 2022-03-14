package com.moko.bxp.button.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.bxp.button.R;
import com.moko.support.entity.AlarmEvent;
import com.moko.support.entity.THStoreData;

public class AlarmEventListAdapter extends BaseQuickAdapter<AlarmEvent, BaseViewHolder> {
    public AlarmEventListAdapter() {
        super(R.layout.item_export_event_data);
    }

    @Override
    protected void convert(BaseViewHolder helper, AlarmEvent item) {
        helper.setText(R.id.tv_time, item.time);
        helper.setText(R.id.tv_trigger_mode, item.triggerMode);
    }
}
