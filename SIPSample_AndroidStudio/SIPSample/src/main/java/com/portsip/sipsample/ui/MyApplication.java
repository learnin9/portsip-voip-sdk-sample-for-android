package com.portsip.sipsample.ui;

import com.portsip.PortSipSdk;

import android.app.Application;

public class MyApplication extends Application {
	public boolean mConference= false;
	public PortSipSdk mEngine;
	public boolean mUseFrontCamera= false;

	@Override
	public void onCreate() {
		super.onCreate();
		mEngine = new PortSipSdk();
	}
}
