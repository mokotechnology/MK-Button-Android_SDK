package com.moko.support.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.entity.OrderCHAR;

public class GetSlotTypeTask extends OrderTask {

    public byte[] data;

    public GetSlotTypeTask() {
        super(OrderCHAR.CHAR_SLOT_TYPE, OrderTask.RESPONSE_TYPE_READ);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
