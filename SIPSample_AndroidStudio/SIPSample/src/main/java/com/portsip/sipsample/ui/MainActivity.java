package com.portsip.sipsample.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;

import android.widget.RadioGroup;
import android.widget.Toast;


import com.portsip.R;
import com.portsip.sipsample.receiver.PortMessageReceiver;
import com.portsip.sipsample.service.PortSipService;


public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {

    public PortMessageReceiver receiver = null;

    private final int REQ_DANGERS_PERMISSION = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiver = new PortMessageReceiver();
        setContentView(R.layout.main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PortSipService.REGISTER_CHANGE_ACTION);
        filter.addAction(PortSipService.CALL_CHANGE_ACTION);
        filter.addAction(PortSipService.PRESENCE_CHANGE_ACTION);
        registerReceiver(receiver, filter);
        switchContent(R.id.login_fragment);
        RadioGroup menuGroup = findViewById(R.id.tab_menu);
        menuGroup.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermissions (this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQ_DANGERS_PERMISSION:
                int i=0;
                for(int result:grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "you must grant the permission "+permissions[i], Toast.LENGTH_SHORT).show();
						i++;
                        stopService(new Intent(this,PortSipService.class));
                        System.exit(0);
                    }
                }
                break;
        }
    }
    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        switch (checkedId) {
            case R.id.tab_login:
                switchContent(R.id.login_fragment);
                break;
            case R.id.tab_numpad:
                switchContent(R.id.numpad_fragment);
                break;
            case R.id.tab_video:
                switchContent(R.id.video_fragment);
                break;
            case R.id.tab_message:
                switchContent(R.id.message_fragment);
                break;
            case R.id.tab_setting:
                switchContent(R.id.setting_fragment);
                break;
        }

    }

    private void switchContent(@IdRes int fragmentId) {
        Fragment fragment = getFragmentManager().findFragmentById(fragmentId);
        Fragment login_fragment = getFragmentManager().findFragmentById(R.id.login_fragment);
        Fragment numpad_fragment = getFragmentManager().findFragmentById(R.id.numpad_fragment);
        Fragment video_fragment = getFragmentManager().findFragmentById(R.id.video_fragment);
        Fragment setting_fragment = getFragmentManager().findFragmentById(R.id.setting_fragment);
        Fragment message_fragment = getFragmentManager().findFragmentById(R.id.message_fragment);

        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        fTransaction.hide(login_fragment).hide(numpad_fragment).hide(video_fragment).hide(setting_fragment).hide(message_fragment);
        if(fragment!=null){
            fTransaction.show( fragment).commit();
        }
    }

    public void requestPermissions(Activity activity) {
        // Check if we have write permission
        if(	PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ||PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                ||PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO))
        {
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},
                    REQ_DANGERS_PERMISSION);
        }
    }

}
