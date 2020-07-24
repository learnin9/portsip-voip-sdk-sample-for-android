package com.portsip.sipsample.ui;

import com.portsip.R;
import com.portsip.sipsample.receiver.PortMessageReceiver;
import com.portsip.sipsample.service.PortSipService;
import com.portsip.sipsample.util.CallManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import static com.portsip.sipsample.service.PortSipService.EXTRA_REGISTER_STATE;

public class LoginFragment extends BaseFragment implements AdapterView.OnItemSelectedListener, View.OnClickListener,PortMessageReceiver.BroadcastListener{
    MyApplication application;
    MainActivity activity;
    private EditText etUsername = null;
    private EditText etPassword = null;
    private EditText etSipServer = null;
    private EditText etSipServerPort = null;

    private EditText etDisplayname = null;

    private EditText etUserdomain = null;
    private EditText etAuthName = null;
    private EditText etStunServer = null;
    private EditText etStunPort = null;
    private Spinner spTransport;
    private Spinner spSRTP;
    private TextView mtxStatus;
    private SharedPreferences mPreferences;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        application = (MyApplication) activity.getApplicationContext();
        View view = inflater.inflate(R.layout.login, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mtxStatus = (TextView) view.findViewById(R.id.txtips);

        etUsername = (EditText) view.findViewById(R.id.etusername);
        etPassword = (EditText) view.findViewById(R.id.etpwd);
        etSipServer = (EditText) view.findViewById(R.id.etsipsrv);
        etSipServerPort = (EditText) view.findViewById(R.id.etsipport);

        etDisplayname = (EditText) view.findViewById(R.id.etdisplayname);
        etUserdomain = (EditText) view.findViewById(R.id.etuserdomain);
        etAuthName = (EditText) view.findViewById(R.id.etauthName);
        etStunServer = (EditText) view.findViewById(R.id.etStunServer);
        etStunPort = (EditText) view.findViewById(R.id.etStunPort);

        spTransport = (Spinner) view.findViewById(R.id.spTransport);
        spSRTP = (Spinner) view.findViewById(R.id.spSRTP);

        spTransport.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.transports, android.R.layout.simple_list_item_1));
        spSRTP.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.srtp, android.R.layout.simple_list_item_1));

        spSRTP.setOnItemSelectedListener(this);
        spTransport.setOnItemSelectedListener(this);

        LoadUserInfo();
        setOnlineStatus(null);

        activity.receiver.broadcastReceiver = this;
        view.findViewById(R.id.btonline).setOnClickListener(this);
        view.findViewById(R.id.btoffline).setOnClickListener(this);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden){
            activity.receiver.broadcastReceiver = this;
            setOnlineStatus(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activity.receiver.broadcastReceiver =null;
    }


    private void LoadUserInfo() {

        etUsername.setText(mPreferences.getString(PortSipService.USER_NAME, null));
        etPassword.setText(mPreferences.getString(PortSipService.USER_PWD, null));
        etSipServer.setText(mPreferences.getString(PortSipService.SVR_HOST, null));
        etSipServerPort.setText(mPreferences.getString(PortSipService.SVR_PORT, "5060"));

        etDisplayname.setText(mPreferences.getString(PortSipService.USER_DISPALYNAME, null));
        etUserdomain.setText(mPreferences.getString(PortSipService.USER_DOMAIN, null));
        etAuthName.setText(mPreferences.getString(PortSipService.USER_AUTHNAME, null));
        etStunServer.setText(mPreferences.getString(PortSipService.STUN_HOST, null));
        etStunPort.setText(mPreferences.getString(PortSipService.STUN_PORT, "3478"));

        spTransport.setSelection(mPreferences.getInt(PortSipService.TRANS, 0));
        spSRTP.setSelection(mPreferences.getInt(PortSipService.SRTP, 0));
    }

    private void SaveUserInfo() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PortSipService.USER_NAME, etUsername.getText().toString());
        editor.putString(PortSipService.USER_PWD, etPassword.getText().toString());
        editor.putString(PortSipService.SVR_HOST, etSipServer.getText().toString());
        editor.putString(PortSipService.SVR_PORT, etSipServerPort.getText().toString());

        editor.putString(PortSipService.USER_DISPALYNAME, etDisplayname.getText().toString());
        editor.putString(PortSipService.USER_DOMAIN, etUserdomain.getText().toString());
        editor.putString(PortSipService.USER_AUTHNAME, etAuthName.getText().toString());
        editor.putString(PortSipService.STUN_HOST, etStunServer.getText().toString());
        editor.putString(PortSipService.STUN_PORT, etStunPort.getText().toString());

        editor.commit();
    }


    public void onBroadcastReceiver(Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (PortSipService.REGISTER_CHANGE_ACTION.equals(action)) {
            String tips  =intent.getStringExtra(EXTRA_REGISTER_STATE);
            setOnlineStatus(tips);
        } else if (PortSipService.CALL_CHANGE_ACTION.equals(action)) {
            //long sessionId = intent.GetLongExtra(PortSipService.EXTRA_CALL_SEESIONID, Session.INVALID_SESSION_ID);
            //callStatusChanged(sessionId);
        }
    }

    private void setOnlineStatus(String tips) {
        if (CallManager.Instance().regist) {
            mtxStatus.setText(TextUtils.isEmpty(tips)?getString(R.string.online):tips);
        } else {
            mtxStatus.setText(TextUtils.isEmpty(tips)?getString(R.string.offline):tips);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btonline:
                SaveUserInfo();
                Intent onLineIntent = new Intent(getActivity(), PortSipService.class);
                onLineIntent.setAction(PortSipService.ACTION_SIP_REGIEST);
                startSipService(onLineIntent);
                mtxStatus.setText("RegisterServer..");
                break;
            case R.id.btoffline:
                Intent offLineIntent = new Intent(getActivity(), PortSipService.class);
                offLineIntent.setAction(PortSipService.ACTION_SIP_UNREGIEST);
                startSipService(offLineIntent);
                mtxStatus.setText("unRegisterServer");
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        if (adapterView == null)
            return;
        SharedPreferences.Editor editor = mPreferences.edit();
        switch (adapterView.getId()) {
            case R.id.spSRTP:
                editor.putInt(PortSipService.SRTP, position).commit();
                break;
            case R.id.spTransport:
                if(mPreferences.getInt(PortSipService.TRANS, 0)!=position) {
                    editor.putInt(PortSipService.TRANS, position).commit();
                    Intent setTransIntent = new Intent(getActivity(), PortSipService.class);
                    setTransIntent.setAction(PortSipService.ACTION_SIP_REINIT);
                    startSipService(setTransIntent);
                }
                break;
        }
    }

    private void startSipService(Intent intent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(intent);
        }else{
            getActivity().startService(intent);
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
