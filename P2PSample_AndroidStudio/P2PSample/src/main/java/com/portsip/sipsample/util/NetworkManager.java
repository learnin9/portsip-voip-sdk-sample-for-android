package com.portsip.sipsample.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Observable;

/**
 * Network service.
 */
public class NetworkManager {
	private static final String OPENVPN_INTERFACE_NAME = "tun0";
	private static final String WIFI_TAG_NAME = "PortGO";
	
	private WifiManager mWifiManager;
	private WifiLock mWifiLock;
	private boolean mAcquired;
	private boolean mStarted;
	boolean mConnect;
	Context mContext;
	private BroadcastReceiver mNetStatusWatcher;
    NetWorkChangeListner mNetWorkChangeListner;
	int mConnectType;
	private static final String[] VPN_INTERFACENAME = {"tun0","ppp0"};//normal tun0 ,vpn ppp0
	private static final String[] WIFI_INTERFACENAME = {"wlan0"};
	public static final int[] sWifiSignalValues = new int[] { 0,1, 2,3, 4};
	static  NetworkManager instance = new NetworkManager();
	static  public NetworkManager getNetWorkmanager(){
		return instance;
	}

	public interface NetWorkChangeListner{
        void handleNetworkChangeEvent(boolean ethernet, boolean wifiConnect, boolean mobileConnect, boolean netTypeChange);
    }


	public static boolean isNetworkAvailable(ConnectivityManager connectivity) {

		if (connectivity== null) {
			return false;
		} else {
			NetworkInfo[] info= connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i= 0; i <info.length; i++) {
					if (info[i].getState() == State.CONNECTED) {
						return true;
						}
					}
				}
		}
		return false;
	}

	public boolean checkNetWorkStatus(){
		boolean connected = false;
		NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (networkInfo == null|| !networkInfo.isAvailable()
			      ||!(networkInfo.getState() == State.CONNECTED)) {
			return false;
		}

		int netType = networkInfo.getType();
		int netSubType = networkInfo.getSubtype();

        boolean useWifi = true;
        boolean use3G = true;

		if(netType== ConnectivityManager.TYPE_ETHERNET&&networkInfo.isConnected()){
			connected = true;
		}else if (useWifi && (netType == ConnectivityManager.TYPE_WIFI)) {
			connected = true;
		} else if (use3G
				&& (netType == ConnectivityManager.TYPE_MOBILE || netType == ConnectivityManager_TYPE_WIMAX)) {
			if ((netSubType >= TelephonyManager.NETWORK_TYPE_UMTS)
					|| // HACK
					(netSubType == TelephonyManager.NETWORK_TYPE_GPRS)
					|| (netSubType == TelephonyManager.NETWORK_TYPE_EDGE)) {
				connected = true;
			}
		}

		if (!connected) {
			return false;
		}
		return connected;
	}

	class myObservalable extends Observable {
		protected void setChangedAndNotifyObservers(Object data){
			super.setChanged();
			super.notifyObservers(data);
		}
	}
	// Will be added in froyo SDK
	private static int ConnectivityManager_TYPE_WIMAX = 6;

	public enum DNS_TYPE {
		DNS_1, DNS_2, DNS_3, DNS_4
	}

	private NetworkManager() {
	}

	public boolean start(Context context) {
        mContext = context;
		mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);

		if(mWifiManager == null){
			return false;
		}
//
		if(mNetStatusWatcher ==null){
			mConnect = false;
			mNetStatusWatcher = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {
                    NetworkInfo networkInfo = null;
					boolean bEthernet = false;
                    boolean bWifiConnect = false;
                    boolean bMobileConnect = false;

                    ConnectivityManager connManager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


					networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
					if(networkInfo!=null&& NetworkInfo.State.CONNECTED == networkInfo.getState()){
						bEthernet = true;
					}

                    networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if(networkInfo!=null&& NetworkInfo.State.CONNECTED == networkInfo.getState()){
                        //wifi
						bWifiConnect = true;//
                    }

                    networkInfo= connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                    if(networkInfo!=null&& NetworkInfo.State.CONNECTED == networkInfo.getState()){
                        //3g
						bMobileConnect = true;
                    }

                    if(mNetWorkChangeListner!=null) {
						mNetWorkChangeListner.handleNetworkChangeEvent(bEthernet, bWifiConnect, bMobileConnect, false);
					}
					//observableObj.setChangedAndNotifyObservers(netType);
				}
			};
		}
		
		if(mNetStatusWatcher!=null)
		{
			IntentFilter intentNetWatcher = new IntentFilter();
			intentNetWatcher.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mNetStatusWatcher, intentNetWatcher);
		}
		
		mStarted = true;
		return true;
	}

	public boolean stop() {
		if(!mStarted){
			return false;
		}
		
//		if(mNetworkWatcher != null){
//			PortSipEngine.getInstance().getApplicationContext().unregisterReceiver(mNetworkWatcher);
//			mNetworkWatcher = null;
//		}
		
		if(mNetStatusWatcher!=null){
            mContext.unregisterReceiver(mNetStatusWatcher);
			mNetStatusWatcher = null;
		}
		
		release();
		mContext = null;
		mStarted = false;
		return true;
	}

	public String getDnsServer(DNS_TYPE type) {
		String dns = null;
		switch (type) {
			case DNS_1: default: dns = "dns1"; break;
			case DNS_2: dns = "dns2"; break;
			case DNS_3: dns = "dns3"; break;
			case DNS_4: dns = "dns4"; break;
		}

		if (mWifiManager != null) {
			String[] dhcpInfos = mWifiManager.getDhcpInfo().toString().split(" ");
			int i = 0;

			while (i++ < dhcpInfos.length) {
				if (dhcpInfos[i - 1].equals(dns)) {
					return dhcpInfos[i];
				}
			}
		}
		return getMobileNetworkDns(type);
	}
	
	String getMobileNetworkDns(DNS_TYPE dnsType){
		String dns;
		Class<?> SystemProperties = null;
		Method method = null;
		
		switch (dnsType) {
		case DNS_1: default: dns = "net.rmnet0.dns1"; break;
		case DNS_2: dns = "net.rmnet0.dns2"; break;
		}
		
		try {
			SystemProperties = Class.forName("android.os.SystemProperties");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		try {
			method = SystemProperties.getMethod("get", String.class);
		} catch (SecurityException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return null;
		}
		
		String value = null;
		try {
			value = (String) method.invoke(null, dns);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	    if (value != null && !"".equals(value) && !value.equals("0.0.0.0"))
	    	return value;
		
		return null;
	}

	public String getLocalIP(boolean ipv6) {
		final HashMap<String, String> addressMap = new HashMap<String, String>();
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						if (((inetAddress instanceof Inet4Address) && !ipv6) || ((inetAddress instanceof Inet6Address) && ipv6)) {
							addressMap.put(intf.getName(), inetAddress.getHostAddress().toString());
						}
					}
				}
			}
						
			if(addressMap.size() > 0){
				for (int i = 0; i < VPN_INTERFACENAME.length; i++) {				
					final String vpnAddr = addressMap.get(VPN_INTERFACENAME[i]);
					if(vpnAddr!=null&&vpnAddr.length()>0){
						return vpnAddr;
					}					
				}
				String ipaddrString = null;
				ipaddrString = getWifiIpAddress();
				if(ipaddrString !=null)
					return ipaddrString;

				return addressMap.values().iterator().next();
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}

		// Hack
		try {
			java.net.Socket socket = new java.net.Socket(ipv6 ? "ipv6.google.com" : "google.com", 80);			
			return socket.getLocalAddress().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	String getWifiIpAddress(){
		if(!mWifiManager.isWifiEnabled())  {  			 
			return null;  			  
			}  
			  
			WifiInfo wifiinfo= mWifiManager.getConnectionInfo();
			String ip=intToIp(wifiinfo.getIpAddress());
			return ip;
	}
	
	private String intToIp(int i)  {
		return (i & 0xFF)+ "." + ((i >> 8 ) & 0xFF)+ "." + ((i >> 16 ) & 0xFF) +"."+((i >> 24 ) & 0xFF);
	}
		
	public boolean acquire() {
		if (mAcquired) {
			//log..e("netWork change", "acquire ok");
			return true;
		}

		boolean connected = false;
		NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (networkInfo == null) {
			return false;
		}

		int netType = networkInfo.getType();
		int netSubType = networkInfo.getSubtype();

        boolean useWifi = true;
        boolean use3G = true;
		if(netType == ConnectivityManager.TYPE_ETHERNET){
			connected = true;
		}else if (useWifi && (netType == ConnectivityManager.TYPE_WIFI)){
			if (mWifiManager != null && mWifiManager.isWifiEnabled()) {
				mWifiLock = mWifiManager.createWifiLock(
						WifiManager.WIFI_MODE_FULL, WIFI_TAG_NAME);
				final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
				if (wifiInfo != null && mWifiLock != null) {
					final DetailedState detailedState = WifiInfo
							.getDetailedStateOf(wifiInfo.getSupplicantState());
					if (detailedState == DetailedState.CONNECTED
							|| detailedState == DetailedState.CONNECTING
							|| detailedState == DetailedState.OBTAINING_IPADDR) {
						mWifiLock.acquire();
//						mConnetedSSID = wifiInfo.getSSID();
						connected = true;
					}
				}
			} else {
				//log.d("", "WiFi not enabled");
			}
		} else if (use3G
				&& (netType == ConnectivityManager.TYPE_MOBILE || netType == ConnectivityManager_TYPE_WIMAX)) {
			if ((netSubType >= TelephonyManager.NETWORK_TYPE_UMTS)
					|| // HACK
					(netSubType == TelephonyManager.NETWORK_TYPE_GPRS)
					|| (netSubType == TelephonyManager.NETWORK_TYPE_EDGE)) {
				connected = true;
			}
		}

		if (!connected) {
			return false;
		}
		mAcquired = true;
		return true;
	}

	public boolean release() {
		if (mWifiLock != null) {
			if(mWifiLock.isHeld()){
				mWifiLock.release();
			}	
			mWifiLock = null;
		}

		mAcquired = false;
		return true;
	}	

    public void setNetWorkChangeListner(NetWorkChangeListner listner){
        mNetWorkChangeListner = listner;
    }

}
