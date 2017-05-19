package com.github.tsiangleo.sr.client.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.tsiangleo.sr.client.service.WatchDogService;

/**
 * Created by tsiang on 2016/12/1.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, WatchDogService.class);
        context.startService(service);
    }
}
