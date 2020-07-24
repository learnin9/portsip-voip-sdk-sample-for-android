package com.portsip.sipsample.service;
import android.content.Intent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;
import static com.portsip.sipsample.service.PortSipService.ACTION_PUSH_MESSAGE;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onCreate() {
        super.onCreate();
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        if(data!=null){
            if ("call".equals(data.get("msg_type")))
            {
                Intent srvIntent = new Intent(this, PortSipService.class);
                srvIntent.setAction(ACTION_PUSH_MESSAGE);
                startService(srvIntent);
            }
            if ("im".equals(data.get("msg_type")))
            {
                String content = data.get("msg_content");
                String from = data.get("send_from");
                String to = data.get("send_to");
                String pushid = data.get("portsip-push-id");//old
                String xpushid = data.get("x-push-id");//new version
                Intent srvIntent = new Intent(this, PortSipService.class);
                srvIntent.setAction(ACTION_PUSH_MESSAGE);
                startService(srvIntent);
            }

        }
    }

    @Override
    public void onNewToken(String s) {
        sendRegistrationToServer(s);
    }

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
    }

    @Override
    public void onSendError(String s, Exception e) {
        super.onSendError(s, e);
    }

    private void sendRegistrationToServer(String token) {
        Intent intent = new Intent(this,PortSipService.class);
        intent.setAction(PortSipService.ACTION_PUSH_TOKEN);
        intent.putExtra(PortSipService.EXTRA_PUSHTOKEN,token);
        startService(intent);
    }
}
