package com.portsip.sipsample.ui;

import com.portsip.PortSipSdk;
import com.portsip.R;
import com.portsip.sipsample.service.PortSipService;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import android.view.View;

public class SettingFragment extends PreferenceFragment {
	MyApplication application;
	MainActivity activity;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		activity = (MainActivity) getActivity();
		application = (MyApplication)activity.getApplication();
		addPreferencesFromResource(R.xml.setting);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(getResources().getColor(R.color.white));
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (hidden) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			PortSipService.ConfigPreferences(getActivity(), preferences, application.mEngine);
		}else{
			activity.receiver.broadcastReceiver =null;
		}
	}
}
