package com.portsip.sipsample.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.portsip.OnPortSIPEvent;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipSdk;
import com.portsip.R;
import com.portsip.sipsample.ui.MainActivity;
import com.portsip.sipsample.ui.MyApplication;
import com.portsip.sipsample.util.CallManager;
import com.portsip.sipsample.util.Contact;
import com.portsip.sipsample.util.ContactManager;
import com.portsip.sipsample.util.Ring;
import com.portsip.sipsample.util.Session;

import java.util.Random;
import java.util.UUID;

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


