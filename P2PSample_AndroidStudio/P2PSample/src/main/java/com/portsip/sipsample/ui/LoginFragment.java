package com.portsip.sipsample.ui;

import com.portsip.R;
import com.portsip.sipsample.receiver.PortMessageReceiver;
import com.portsip.sipsample.service.PortSipService;
import com.portsip.sipsample.util.CallManager;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import androidx.annotation.Nullable;

public class LoginFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener,PortMessageReceiver.BroadcastListener{
    MyApplication application;
    MainActivity activity;
    private EditText etUsername = null;
    private EditText etPassword = null;
    private EditText etSipServer = null;
    private EditText etSipServerPort = null;

    private EditText etDisplayname = null;

    private Spinner spSRTP;
    private TextView mtxStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
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

        etSipServer.setText(getLocalIP(false));
        etSipServer.setEnabled(false);

        etSipServerPort = (EditText) view.findViewById(R.id.etsipport);

        etDisplayname = (EditText) view.findViewById(R.id.etdisplayname);
        spSRTP = (Spinner) view.findViewById(R.id.spSRTP);

        spSRTP.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.srtp, android.R.layout.simple_list_item_1));

        spSRTP.setOnItemSelectedListener(this);

        loadUserInfo();
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


    private void loadUserInfo() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        etUsername.setText(preferences.getString(PortSipService.USER_NAME, null));
        etPassword.setText(preferences.getString(PortSipService.USER_PWD, null));
//        etSipServer.setText(preferences.getString(PortSipService.SVR_HOST, null));
        etSipServerPort.setText(preferences.getString(PortSipService.SVR_PORT, "6060"));

        etDisplayname.setText(preferences.getString(PortSipService.USER_DISPALYNAME, null));

        spSRTP.setSelection(preferences.getInt(PortSipService.SRTP, 0));
    }

    private void saveUserInfo() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putString(PortSipService.USER_NAME, etUsername.getText().toString());
        editor.putString(PortSipService.USER_PWD, etPassword.getText().toString());
        editor.putString(PortSipService.SVR_HOST, etSipServer.getText().toString());
        editor.putString(PortSipService.SVR_PORT, etSipServerPort.getText().toString());

        editor.putString(PortSipService.USER_DISPALYNAME, etDisplayname.getText().toString());

        editor.commit();
    }


    public void onBroadcastReceiver(Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (PortSipService.REGISTER_CHANGE_ACTION.equals(action)) {
            String tips  =intent.getStringExtra(EXTRA_REGISTER_STATE);
            etSipServer.setText(getLocalIP(false));
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
                saveUserInfo();
                Intent onLineIntent = new Intent(getActivity(), PortSipService.class);
                onLineIntent.setAction(PortSipService.ACTION_SIP_REGIEST);
                getActivity().startService(onLineIntent);
                mtxStatus.setText("RegisterServer..");
                break;
            case R.id.btoffline:
                Intent offLineIntent = new Intent(getActivity(), PortSipService.class);
                offLineIntent.setAction(PortSipService.ACTION_SIP_UNREGIEST);
                getActivity().startService(offLineIntent);
                mtxStatus.setText("unRegisterServer");
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        if (adapterView == null)
            return;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        switch (adapterView.getId()) {
            case R.id.spSRTP:
                editor.putInt(PortSipService.SRTP, position).commit();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private static final String[] INTERFACE_ORDER = {"tun0","ppp0","wlan0"};
    public String getLocalIP(boolean ipv6) {

        final Map<String, String> addressMap = new IdentityHashMap<String, String>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (((inetAddress instanceof Inet4Address) && !ipv6) || ((inetAddress instanceof Inet6Address) && ipv6)) {
                            StringTokenizer token = new StringTokenizer(inetAddress.getHostAddress().toString(), "%");
                            addressMap.put(new String(intf.getName()), token.nextToken());
                        }
                    }
                }
            }
            if(addressMap.size() > 0){
                String address =null;
                for (int i = 0; i < INTERFACE_ORDER.length; i++) {
                    Iterator iter = addressMap.keySet().iterator();
                    while (iter.hasNext()) {
                        Object key = iter.next();
                        if(key.equals(INTERFACE_ORDER[i])) {
                            String val = addressMap.get(key);
                            if (val != null && val.length() > 0) {
                                if (val.startsWith("fe80")) {
                                    continue;
                                }
                                return address = val;
                            }
                        }
                    }
                }

                address = addressMap.values().iterator().next();
                return address;
            }
        } catch (SocketException ex) {
        }

        return null;
    }

}
