package com.portsip.sipsample.service;

import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;
import com.portsip.OnPortSIPEvent;
import com.portsip.R;

import com.portsip.sipsample.ui.IncomingActivity;
import com.portsip.sipsample.ui.MainActivity;
import com.portsip.sipsample.ui.MyApplication;
import com.portsip.sipsample.util.CallManager;
import com.portsip.sipsample.util.Contact;
import com.portsip.sipsample.util.ContactManager;
import com.portsip.sipsample.util.NetworkManager;
import com.portsip.sipsample.util.Ring;
import com.portsip.sipsample.util.Session;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import androidx.core.app.NotificationCompat;


public class PortSipService extends Service implements OnPortSIPEvent, NetworkManager.NetWorkChangeListner {
    public static final String ACTION_SIP_REGIEST = "PortSip.AndroidSample.Test.REGIEST";
    public static final String ACTION_SIP_UNREGIEST = "PortSip.AndroidSample.Test.UNREGIEST";

    public static final String INSTANCE_ID = "instanceid";

    public static final String USER_NAME = "user name";
    public static final String USER_PWD = "user pwd";
    public static final String SVR_HOST = "svr host";
    public static final String SVR_PORT = "svr port";

    public static final String USER_DOMAIN = "user domain";
    public static final String USER_DISPALYNAME = "user dispalay";
    public static final String USER_AUTHNAME = "user authname";
    public static final String STUN_HOST = "stun host";
    public static final String STUN_PORT = "stun port";

    public static final String TRANS = "trans type";
    public static final String SRTP = "srtp type";

    protected PowerManager.WakeLock mCpuLock;
    public static final String REGISTER_CHANGE_ACTION = "PortSip.AndroidSample.Test.RegisterStatusChagnge";
    public static final String CALL_CHANGE_ACTION = "PortSip.AndroidSample.Test.CallStatusChagnge";
    public static final String PRESENCE_CHANGE_ACTION = "PortSip.AndroidSample.Test.PRESENCEStatusChagnge";

    public static String EXTRA_REGISTER_STATE = "RegisterStatus";
    public static String EXTRA_CALL_SEESIONID = "SessionID";
    public static String EXTRA_CALL_DESCRIPTION = "Description";

    private PortSipSdk mEngine;
    private MyApplication applicaton;

	private static final int SERVICE_NOTIFICATION  = 31414;
    public  static final int PENDINGCALL_NOTIFICATION= SERVICE_NOTIFICATION+1;
    private String ChannelID = "PortSipService";


    private NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        applicaton = (MyApplication) getApplicationContext();
        mEngine = applicaton.mEngine;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(ChannelID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            mNotificationManager.createNotificationChannel(channel);
        }
        showServiceNotifiCation();
        if(NetworkManager.getNetWorkmanager().start(this)){
            NetworkManager.getNetWorkmanager().setNetWorkChangeListner(this);
        }

    }

    private void showServiceNotifiCation(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0/*requestCode*/, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this,ChannelID);
        }else{
            builder = new Notification.Builder(this);
        }
        builder.setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service Running")
                .setContentIntent(contentIntent)
                .build();// getNotification()
        startForeground(SERVICE_NOTIFICATION,builder.build());
    }
    public void showPendingCallNotification(Context context, String contenTitle,String contenText,Intent intent) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ChannelID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(contenTitle)
                .setContentText(contenText)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setContentIntent(contentIntent)
                .setFullScreenIntent(contentIntent, true);
        mNotificationManager.notify(PENDINGCALL_NOTIFICATION, builder.build());
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            if (ACTION_SIP_REGIEST.equals(intent.getAction()) && !CallManager.Instance().regist) {
                registerToServer();
            } else if (ACTION_SIP_UNREGIEST.equals(intent.getAction())) {
                unregisterToServer();
            }

        }
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEngine.destroyConference();
        mEngine = null;
        applicaton.mEngine = null;

        if (mCpuLock != null) {
            mCpuLock.release();
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(ChannelID);
        }
        NetworkManager.getNetWorkmanager().stop();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void registerToServer() {
        SharedPreferences prefences = PreferenceManager.getDefaultSharedPreferences(this);
        String port = prefences.getString(SVR_PORT, "6060");

        int srtptype = prefences.getInt(SRTP, 0);

        String registerTips = "initialize failed";
        int result = 0;
        mEngine.DeleteCallManager();
        mEngine.CreateCallManager(applicaton);
        mEngine.setOnPortSIPEvent(this);
        String dataPath = getExternalFilesDir(null).getAbsolutePath();
        String certRoot = dataPath + "/certs";

        int localport = Integer.parseInt(port);
        result = mEngine.initialize(PortSipEnumDefine.ENUM_TRANSPORT_UDP, "0.0.0.0", localport,
                PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG, dataPath,
                8, "PortSIP SDK for Android", 0, 0, certRoot, "", false, null);
        if (result == PortSipErrorcode.ECoreErrorNone) {
            //init failed
            registerTips = "ECoreWrongLicenseKey";
            result = mEngine.setLicenseKey("LicenseKey");
            if (result != PortSipErrorcode.ECoreWrongLicenseKey) {

                mEngine.getAudioDevices();
                mEngine.setVideoDeviceId(1);
                mEngine.setSrtpPolicy(srtptype);
                ConfigPresence(this, prefences, mEngine);

                mEngine.enable3GppTags(false);

                String name = prefences.getString(USER_NAME, "");
                String displayname = prefences.getString(USER_DISPALYNAME, "");
                String pwd = prefences.getString(USER_PWD, "");

                registerTips = "invalidate user info";
                result = -1;
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(pwd)) {

                    result = mEngine.setUser(name, displayname, null, pwd,
                            null, null, 0, null, 0, null, 0);
                    registerTips = "setUser failed";
                    if (result == PortSipErrorcode.ECoreErrorNone) {

                        mEngine.setInstanceId(getInstanceID());
                        onRegisterSuccess("online", result, "");

                    }
                }
            }
        }

        if (result != PortSipErrorcode.ECoreErrorNone) {
            onRegisterFailure(registerTips, result, "");
        }
    }

    public static void ConfigPresence(Context context, SharedPreferences prefences, PortSipSdk sdk) {
        sdk.clearAudioCodec();
        if (prefences.getBoolean(context.getString(R.string.MEDIA_G722), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G722);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_PCMA), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMA);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_PCMU), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_G729), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G729);
        }

        if (prefences.getBoolean(context.getString(R.string.MEDIA_GSM), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_GSM);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_ILBC), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ILBC);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_AMR), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_AMR);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_AMRWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_AMRWB);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_SPEEX), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_SPEEX);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_SPEEXWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_SPEEXWB);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_ISACWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ISACWB);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_ISACSWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ISACSWB);
        }
//        if (prefences.getBoolean(context.getString(R.string.MEDIA_G7221), false)) {
//            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G7221);
//        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_OPUS), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_OPUS);
        }

        sdk.clearVideoCodec();
        if (prefences.getBoolean(context.getString(R.string.MEDIA_H264), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_H264);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_VP8), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP8);
        }
        if (prefences.getBoolean(context.getString(R.string.MEDIA_VP9), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP9);
        }
		sdk.enableAEC(prefences.getBoolean(context.getString(R.string.MEDIA_AEC), true));
        sdk.enableAGC(prefences.getBoolean(context.getString(R.string.MEDIA_AGC), true));
        sdk.enableCNG(prefences.getBoolean(context.getString(R.string.MEDIA_CNG), true));
        sdk.enableVAD(prefences.getBoolean(context.getString(R.string.MEDIA_VAD), true));
        sdk.enableANS(prefences.getBoolean(context.getString(R.string.MEDIA_ANS), false));

        boolean foward = prefences.getBoolean(context.getString(R.string.str_fwopenkey), false);
        boolean fowardBusy = prefences.getBoolean(context.getString(R.string.str_fwbusykey), false);
        String fowardto = prefences.getString(context.getString(R.string.str_fwtokey), null);
        if (foward && !TextUtils.isEmpty(fowardto)) {
            sdk.enableCallForward(fowardBusy, fowardto);
        }

        sdk.enableReliableProvisional(prefences.getBoolean(context.getString(R.string.str_pracktitle), false));

        String resolution = prefences.getString((context.getString(R.string.str_resolution)), "CIF");
        int width = 352;
        int height = 288;
        if (resolution.equals("QCIF")) {
            width = 176;
            height = 144;
        } else if (resolution.equals("CIF")) {
            width = 352;
            height = 288;
        } else if (resolution.equals("VGA")) {
            width = 640;
            height = 480;
        } else if (resolution.equals("720P")) {
            width = 1280;
            height = 720;
        } else if (resolution.equals("1080P")) {
            width = 1920;
            height = 1080;
        }

        sdk.setVideoResolution(width, height);
    }

    private int getTransType(int select) {
        switch (select) {
            case 0:
                return PortSipEnumDefine.ENUM_TRANSPORT_UDP;
            case 1:
                return PortSipEnumDefine.ENUM_TRANSPORT_TLS;
            case 2:
                return PortSipEnumDefine.ENUM_TRANSPORT_TCP;
            case 3:
                return PortSipEnumDefine.ENUM_TRANSPORT_PERS_UDP;
            case 4:
                return PortSipEnumDefine.ENUM_TRANSPORT_PERS_TCP;
        }
        return PortSipEnumDefine.ENUM_TRANSPORT_UDP;
    }

    String getInstanceID() {
        SharedPreferences prefences = PreferenceManager.getDefaultSharedPreferences(this);

        String insanceid = prefences.getString(INSTANCE_ID, "");
        if (TextUtils.isEmpty(insanceid)) {
            insanceid = UUID.randomUUID().toString();
            prefences.edit().putString(INSTANCE_ID, insanceid).commit();
        }
        return insanceid;
    }

    public void UnregisterToServerNoPush() {

//        if (!TextUtils.isEmpty(FirebaseInstanceId.Instance.Token)) {
//            String pushMessage = "device-os=android;device-uid=" + FirebaseInstanceId.Instance.Token + ";allow-call-push=false;allow-message-push=true;app-id=" + APPID;
//            mEngine.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", pushMessage);
//        }

        mEngine.unRegisterServer();
        mEngine.DeleteCallManager();
        CallManager.Instance().regist = false;
    }

    public void unregisterToServer() {

        mEngine.DeleteCallManager();
        CallManager.Instance().regist = false;
        onRegisterFailure("offline",PortSipErrorcode.ECoreErrorNone,"");
    }

    //--------------------
    @Override
    public void onRegisterSuccess(String statusText, int statusCode, String sipMessage) {
        CallManager.Instance().regist = true;
        Intent broadIntent = new Intent(REGISTER_CHANGE_ACTION);
        broadIntent.putExtra(EXTRA_REGISTER_STATE,statusText);
        sendPortSipMessage("onRegisterSuccess", broadIntent);
        CallManager.Instance().regist=true;
        keepCpuRun(true);
    }

    @Override
    public void onRegisterFailure(String statusText, int statusCode, String sipMessage) {
        Intent broadIntent = new Intent(REGISTER_CHANGE_ACTION);
        broadIntent.putExtra(EXTRA_REGISTER_STATE, statusText);
        sendPortSipMessage("onRegisterFailure" + statusCode, broadIntent);
        CallManager.Instance().regist=false;
        CallManager.Instance().resetAll();

        keepCpuRun(false);
    }

    @Override
    public void onInviteIncoming(long sessionId,
                                 String callerDisplayName,
                                 String caller,
                                 String calleeDisplayName,
                                 String callee,
                                 String audioCodecNames,
                                 String videoCodecNames,
                                 boolean existsAudio,
                                 boolean existsVideo,
                                 String sipMessage) {


        if(CallManager.Instance().findIncomingCall()!=null){
            applicaton.mEngine.rejectCall(sessionId,486);//busy
            return;
        }
        Session session = CallManager.Instance().findIdleSession();
        session.state = Session.CALL_STATE_FLAG.INCOMING;
        session.hasVideo = existsVideo;
        session.sessionID = sessionId;
        session.remote = caller;
        session.displayName = callerDisplayName;

        Intent activityIntent = new Intent(this, IncomingActivity.class);
        activityIntent.putExtra("incomingSession",sessionId);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if(isForeground()){
            startActivity(activityIntent);
        }else{  
            showPendingCallNotification(this, callerDisplayName, caller, activityIntent);
        }
        Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
        broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);

        String description = session.lineName + " onInviteIncoming";
        broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

        sendPortSipMessage(description, broadIntent);


        Ring.getInstance(this).startRingTone();
    }

    @Override
    public void onInviteTrying(long sessionId) {
    }

    @Override
    public void onInviteSessionProgress(
            long sessionId,
            String audioCodecNames,
            String videoCodecNames,
            boolean existsEarlyMedia,
            boolean existsAudio,
            boolean existsVideo,
            String sipMessage) {
    }

    @Override
    public void onInviteRinging(long sessionId, String statusText, int statusCode, String sipMessage) {
    }

    @Override
    public void onInviteAnswered(long sessionId,
                                 String callerDisplayName,
                                 String caller,
                                 String calleeDisplayName,
                                 String callee,
                                 String audioCodecNames,
                                 String videoCodecNames,
                                 boolean existsAudio,
                                 boolean existsVideo,
                                 String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);


        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED;
            session.hasVideo = existsVideo;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);

            String description = session.lineName + " onInviteAnswered";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }

        Ring.getInstance(this).stopRingBackTone();
    }

    @Override
    public void onInviteFailure(long sessionId, String reason, int code, String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.FAILED;
            session.sessionID = sessionId;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);
            String description = session.lineName + " onInviteFailure";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }

        Ring.getInstance(this).stopRingBackTone();
    }

    @Override
    public void onInviteUpdated(
            long sessionId,
            String audioCodecNames,
            String videoCodecNames,
            boolean existsAudio,
            boolean existsVideo,
            String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);

        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED;
            session.hasVideo = existsVideo;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);
            String description = session.lineName + " OnInviteUpdated";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }
    }

    @Override
    public void onInviteConnected(long sessionId) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED;
            session.sessionID = sessionId;

            if (applicaton.mConference)
            {
                applicaton.mEngine.joinToConference(session.sessionID);
            }

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);
            String description = session.lineName + " OnInviteConnected";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }

        CallManager.Instance().setSpeakerOn(applicaton.mEngine,CallManager.Instance().isSpeakerOn());
        mNotificationManager.cancel(PENDINGCALL_NOTIFICATION);
    }

    @Override
    public void onInviteBeginingForward(String forwardTo) {
    }

    @Override
    public void onInviteClosed(long sessionId) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED;
            session.sessionID = sessionId;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);
            String description = session.lineName + " OnInviteClosed";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }
        Ring.getInstance(this).stopRingTone();
        mNotificationManager.cancel(PENDINGCALL_NOTIFICATION);
    }

    @Override
    public void onDialogStateUpdated(String BLFMonitoredUri,
                                     String BLFDialogState,
                                     String BLFDialogId,
                                     String BLFDialogDirection) {
        String text = "The user ";
        text += BLFMonitoredUri;
        text += " dialog state is updated: ";
        text += BLFDialogState;
        text += ", dialog id: ";
        text += BLFDialogId;
        text += ", direction: ";
        text += BLFDialogDirection;
    }

    @Override
    public void onRemoteUnHold(
            long sessionId,
            String audioCodecNames,
            String videoCodecNames,
            boolean existsAudio,
            boolean existsVideo) {
    }

    @Override
    public void onRemoteHold(long sessionId) {
    }

    @Override
    public void onReceivedRefer(
            long sessionId,
            long referId,
            String to,
            String referFrom,
            String referSipMessage) {
    }

    @Override
    public void onReferAccepted(long sessionId) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED;
            session.sessionID = sessionId;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);
            String description = session.lineName + " onReferAccepted";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }
        Ring.getInstance(this).stopRingTone();
    }

    @Override
    public void onReferRejected(long sessionId, String reason, int code) {
    }

    @Override
    public void onTransferTrying(long sessionId) {
    }

    @Override
    public void onTransferRinging(long sessionId) {
    }

    @Override
    public void onACTVTransferSuccess(long sessionId) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED;
            session.sessionID = sessionId;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);
            String description = session.lineName + " Transfer succeeded, call closed";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
            // Close the call after succeeded transfer the call
            mEngine.hangUp(sessionId);
        }
    }

    @Override
    public void onACTVTransferFailure(long sessionId, String reason, int code) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId);
            String description = session.lineName + " Transfer failure!";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);

        }
    }

    @Override
    public void onReceivedSignaling(long sessionId, String signaling) {
    }

    @Override
    public void onSendingSignaling(long sessionId, String signaling) {
    }

    @Override
    public void onWaitingVoiceMessage(
            String messageAccount,
            int urgentNewMessageCount,
            int urgentOldMessageCount,
            int newMessageCount,
            int oldMessageCount) {
    }

    @Override
    public void onWaitingFaxMessage(
            String messageAccount,
            int urgentNewMessageCount,
            int urgentOldMessageCount,
            int newMessageCount,
            int oldMessageCount) {
    }

    @Override
    public void onRecvDtmfTone(long sessionId, int tone) {
    }

    @Override
    public void onRecvOptions(String optionsMessage) {
    }

    @Override
    public void onRecvInfo(String infoMessage) {
    }

    @Override
    public void onRecvNotifyOfSubscription(long sessionId, String notifyMessage, byte[] messageData, int messageDataLength) {
    }

    //Receive a new subscribe
    @Override
    public void onPresenceRecvSubscribe(
            long subscribeId,
            String fromDisplayName,
            String from,
            String subject) {
        Contact contact = ContactManager.Instance().findContactBySipAddr(from);
        if (contact == null) {
            contact = new Contact();
            contact.sipAddr = from;
            ContactManager.Instance().AddContact(contact);
        }

        contact.subRequestDescription = subject;
        contact.SubId = subscribeId;
        switch (contact.state) {
            case ACCEPTED://This subscribe has accepted
                applicaton.mEngine.presenceAcceptSubscribe(subscribeId);
                break;
            case REJECTED://This subscribe has rejected
                applicaton.mEngine.presenceRejectSubscribe(subscribeId);
                break;
            case UNSETTLLED:
                break;
            case UNSUBSCRIBE:
                contact.state = Contact.SUBSCRIBE_STATE_FLAG.UNSETTLLED;
                break;
        }
        Intent broadIntent = new Intent(PRESENCE_CHANGE_ACTION);
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent);
    }

    //update online status
    @Override
    public void onPresenceOnline(String fromDisplayName, String from, String stateText) {
        Contact contact = ContactManager.Instance().findContactBySipAddr(from);
        if (contact == null) {

        } else {
            contact.subDescription = stateText;
        }

        Intent broadIntent = new Intent(PRESENCE_CHANGE_ACTION);
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent);
    }

    //update offline status
    @Override
    public void onPresenceOffline(String fromDisplayName, String from) {
        Contact contact = ContactManager.Instance().findContactBySipAddr(from);
        if (contact == null) {

        } else {
            contact.subDescription = "Offline";
        }

        Intent broadIntent = new Intent(PRESENCE_CHANGE_ACTION);
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent);
    }

    @Override
    public void onRecvMessage(
            long sessionId,
            String mimeType,
            String subMimeType,
            byte[] messageData,
            int messageDataLength) {
    }

    @Override
    public void onRecvOutOfDialogMessage(
            String fromDisplayName,
            String from,
            String toDisplayName,
            String to,
            String mimeType,
            String subMimeType,
            byte[] messageData,
            int messageDataLengthsipMessage,
            String sipMessage) {
        if ("text".equals(mimeType) && "plain".equals(subMimeType)) {
            Toast.makeText(this,"you have a mesaage from: "+from+ "  "+new String(messageData),Toast.LENGTH_SHORT).show();
        }else{
        }
    }

    @Override
    public void onSendMessageSuccess(long sessionId, long messageId) {
    }

    @Override
    public void onSendMessageFailure(long sessionId, long messageId, String reason, int code) {
    }

    @Override
    public void onSendOutOfDialogMessageSuccess(long messageId,
                                                String fromDisplayName,
                                                String from,
                                                String toDisplayName,
                                                String to) {
    }

    @Override
    public void onSendOutOfDialogMessageFailure(
            long messageId,
            String fromDisplayName,
            String from,
            String toDisplayName,
            String to,
            String reason,
            int code) {
    }

    @Override
    public void onSubscriptionFailure(long subscribeId, int statusCode) {
    }

    @Override
    public void onSubscriptionTerminated(long subscribeId) {
    }

    @Override
    public void onPlayAudioFileFinished(long sessionId, String fileName) {
    }

    @Override
    public void onPlayVideoFileFinished(long sessionId) {
    }

    @Override
    public void onReceivedRTPPacket(
            long sessionId,
            boolean isAudio,
            byte[] RTPPacket,
            int packetSize) {
    }

    @Override
    public void onSendingRTPPacket(long l, boolean b, byte[] bytes, int i) {

    }

    @Override
    public void onAudioRawCallback(
            long sessionId,
            int callbackType,
            byte[] data,
            int dataLength,
            int samplingFreqHz) {
    }

    @Override
    public void onVideoRawCallback(long l, int i, int i1, int i2, byte[] bytes, int i3) {

    }


    //--------------------
    public void sendPortSipMessage(String message, Intent broadIntent) {
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this,ChannelID);
        }else{
            builder = new Notification.Builder(this);
        }
        builder.setSmallIcon(R.drawable.icon)
                .setContentTitle("Sip Notify")
                .setContentText(message)
                .setContentIntent(contentIntent)
                .build();// getNotification()

        mNotifyMgr.notify(1,  builder.build());

        sendBroadcast(broadIntent);
    }

    public int outOfDialogRefer(int replaceSessionId, String replaceMethod, String target, String referTo) {
        return 0;
    }

    public void keepCpuRun(boolean keepRun) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (keepRun == true) { //open
            if (mCpuLock == null) {
                if ((mCpuLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName()+":SipSampleCpuLock")) == null) {
                    return;
                }
                mCpuLock.setReferenceCounted(false);
            }

            synchronized (mCpuLock) {
                if (!mCpuLock.isHeld()) {
                    mCpuLock.acquire();
                }
            }
        } else {//close
            if (mCpuLock != null) {
                synchronized (mCpuLock) {
                    if (mCpuLock.isHeld()) {
                        mCpuLock.release();
                    }
                }
            }
        }
    }

    @Override
    public void handleNetworkChangeEvent(boolean ethernet,boolean wifiConnect,boolean mobileConnect,boolean netTypeChange) {
        mEngine.refreshRegistration(0);
    }
    private boolean isForeground(){
        String[] activitys;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            activitys = getActivePackages(this);
        }else{
            activitys = getActivePackagesCompat(this);
        }
        if(activitys.length>0){
            String packagename = getPackageName();
            //String processName= getProcessName();||activityname.contains(processName)
            for(String activityname:activitys){

                if(activityname.contains(packagename)){
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private String[] getActivePackagesCompat(Context context) {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
        final ComponentName componentName = taskInfo.get(0).topActivity;
        final String[] activePackages = new String[1];
        activePackages[0] = componentName.getPackageName();
        return activePackages;
    }

    private String[] getActivePackages(Context context) {
        final Set<String> activePackages = new HashSet<String>();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                activePackages.addAll(Arrays.asList(processInfo.pkgList));
            }
        }
        return activePackages.toArray(new String[activePackages.size()]);
    }
}

