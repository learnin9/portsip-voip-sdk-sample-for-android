package com.portsip.sipsample.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;

import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;
import com.portsip.R;
import com.portsip.sipsample.receiver.PortMessageReceiver;
import com.portsip.sipsample.service.PortSipService;
import com.portsip.sipsample.util.CallManager;
import com.portsip.sipsample.util.Ring;
import com.portsip.sipsample.util.Session;

public class IncomingActivity extends Activity implements PortMessageReceiver.BroadcastListener, View.OnClickListener {

    public PortMessageReceiver receiver = null;
    MyApplication application;
    TextView tips;
    Button btnVideo;
    long mSessionid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.incomingview);
        tips = findViewById(R.id.sessiontips);
        btnVideo = findViewById(R.id.answer_video);
        receiver = new PortMessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PortSipService.REGISTER_CHANGE_ACTION);
        filter.addAction(PortSipService.CALL_CHANGE_ACTION);
        filter.addAction(PortSipService.PRESENCE_CHANGE_ACTION);
        registerReceiver(receiver, filter);
        receiver.broadcastReceiver =this;
        Intent intent = getIntent();
        mSessionid = intent.getLongExtra("incomingSession", PortSipErrorcode.INVALID_SESSION_ID);
        Session session = CallManager.Instance().findSessionBySessionID(mSessionid);
        if(mSessionid==PortSipErrorcode.INVALID_SESSION_ID||session ==null||session.state!= Session.CALL_STATE_FLAG.INCOMING){
            this.finish();
        }

        application = (MyApplication) getApplication();
        tips.setText(session.lineName+"   "+session.remote );
        setVideoAnswerVisibility(session);

        findViewById(R.id.hangup_call).setOnClickListener(this);
		findViewById(R.id.answer_audio).setOnClickListener(this);
        btnVideo.setOnClickListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        long sessionid = intent.getLongExtra("incomingSession", PortSipErrorcode.INVALID_SESSION_ID);
        Session session = CallManager.Instance().findSessionBySessionID(sessionid);
        if(mSessionid!=PortSipErrorcode.INVALID_SESSION_ID&&session !=null){
            mSessionid = sessionid;
            setVideoAnswerVisibility(session);
            tips.setText(session.lineName+"   "+session.remote );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        startActivity(new Intent(this,MainActivity.class));
    }

    @Override
    public void onBroadcastReceiver(Intent intent) {
        String action = intent.getAction();
        if (PortSipService.CALL_CHANGE_ACTION.equals(action))
        {
            long sessionId = intent.getLongExtra(PortSipService.EXTRA_CALL_SEESIONID, Session.INVALID_SESSION_ID);
            String status = intent.getStringExtra(PortSipService.EXTRA_CALL_DESCRIPTION);
            Session session = CallManager.Instance().findSessionBySessionID(sessionId);
            if (session != null)
            {
                switch (session.state)
                {
                    case INCOMING:
                        break;
                    case TRYING:
                        break;
                    case CONNECTED:
                    case FAILED:
                    case CLOSED:
                        Session anOthersession = CallManager.Instance().findIncomingCall();
                        if(anOthersession==null) {
                            this.finish();
                        }else{
                            setVideoAnswerVisibility(anOthersession);
                            tips.setText(anOthersession.lineName+"   "+anOthersession.remote );
                            mSessionid = anOthersession.sessionID;
                        }
                        break;

                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        if(application.mEngine!=null){

            Session currentLine = CallManager.Instance().findSessionBySessionID(mSessionid);
            switch (view.getId()){
                case R.id.answer_audio:
                case R.id.answer_video:
                    if (currentLine.state != Session.CALL_STATE_FLAG.INCOMING) {
                        Toast.makeText(this,currentLine.lineName + "No incoming call on current line",Toast.LENGTH_SHORT);
                        return;
                    }
                    Ring.getInstance(this).stopRingTone();
                    currentLine.state = Session.CALL_STATE_FLAG.CONNECTED;
                    application.mEngine.answerCall(mSessionid,view.getId()==R.id.answer_video);
                    if(application.mConference){
                        application.mEngine.joinToConference(currentLine.sessionID);
                    }
                    break;
                case R.id.hangup_call:
                    Ring.getInstance(this).stop();
                    if (currentLine.state == Session.CALL_STATE_FLAG.INCOMING) {
                        application.mEngine.rejectCall(currentLine.sessionID, 486);
                        currentLine.reset();
                        Toast.makeText(this,currentLine.lineName + ": Rejected call",Toast.LENGTH_SHORT);
                    }

                    break;
            }
        }

        Session anOthersession = CallManager.Instance().findIncomingCall();
        if(anOthersession==null) {
            this.finish();
        }else{
            mSessionid = anOthersession.sessionID;
            setVideoAnswerVisibility(anOthersession);
        }

    }
	
	private void setVideoAnswerVisibility(Session session){
		if(session == null)
			return;
		if(session.hasVideo){
            btnVideo.setVisibility(View.VISIBLE);
        }else{
            btnVideo.setVisibility(View.GONE);
        }
	}
	
}
