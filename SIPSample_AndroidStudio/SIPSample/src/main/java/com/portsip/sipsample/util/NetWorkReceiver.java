package com.portsip.sipsample.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;

public class NetWorkReceiver extends BroadcastReceiver{
        private static NetWorkListener mListener;

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) && null != context) {
                    int netWorkState = getNetWorkState(context);
                    // 当网络发生变化，判断当前网络状态，并通过NetEvent回调当前网络状态
                    if (mListener != null) {
                        mListener.onNetworkChange(netWorkState);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 自定义接口
        public interface NetWorkListener {
            public void onNetworkChange(int netMobile);
        }

        private int getNetWorkState(@NonNull Context context) {
        // 得到连接管理器对象
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {

            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetworkInfo = connectivityManager
                    .getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                if (activeNetworkInfo.getType() == (ConnectivityManager.TYPE_WIFI)) {
                    return ConnectivityManager.TYPE_WIFI;
                } else if (activeNetworkInfo.getType() == (ConnectivityManager.TYPE_WIFI)) {
                    return ConnectivityManager.TYPE_WIFI;
                }
            } else {
                return -1;
            }

        }else{
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            //获取所有网络连接的信息
            Network[] networks = connMgr.getAllNetworks();
            //通过循环将网络信息逐个取出来
            for (int i=0; i < networks.length; i++) {
                //获取ConnectivityManager对象对应的NetworkInfo对象
                NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);
                if (networkInfo.isConnected()) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        return ConnectivityManager.TYPE_MOBILE;
                    } else {
                        return ConnectivityManager.TYPE_WIFI;
                    }
                }
            }
        }
        return -1;
    }

    public static void setListener(NetWorkListener listener) {
        mListener = listener;
    }
}
