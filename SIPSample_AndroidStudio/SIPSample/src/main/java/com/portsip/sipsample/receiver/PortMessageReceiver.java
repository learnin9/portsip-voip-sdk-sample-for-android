package com.portsip.sipsample.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class PortMessageReceiver extends BroadcastReceiver
{
    public interface BroadcastListener {void onBroadcastReceiver(Intent intent);}
    @Override
    public void onReceive(Context context, Intent intent) {
        if(broadcastReceiver!=null) {
            broadcastReceiver.onBroadcastReceiver(intent);
        }
    }
    public BroadcastListener broadcastReceiver;
}


