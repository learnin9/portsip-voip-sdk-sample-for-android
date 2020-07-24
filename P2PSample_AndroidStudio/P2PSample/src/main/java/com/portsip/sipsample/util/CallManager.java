package com.portsip.sipsample.util;

import com.portsip.PortSIPVideoRenderer;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipSdk;

public class CallManager
{
	public static final int MAX_LINES = 10;
	private static CallManager mInstance;
	private static Object locker = new Object();
	Session[] sessions;
	public int CurrentLine;
	public boolean regist;
	public boolean speakerOn =false;

	public boolean setSpeakerOn(PortSipSdk portSipSdk ,boolean speakerOn) {
		this.speakerOn = speakerOn;
		if(speakerOn) {
			portSipSdk.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE);
		}else{
			portSipSdk.setAudioDevice(PortSipEnumDefine.AudioDevice.EARPIECE);
		}
		return speakerOn;
	}

	public boolean isSpeakerOn() {
		return speakerOn;
	}

	public static CallManager Instance()
	{
			if (mInstance == null)
			{
				synchronized (locker)
				{
					if (mInstance == null)
					{
						mInstance = new CallManager();
					}
				}
			}

			return mInstance;
	}
	private CallManager()
	{
		CurrentLine = 0;
		sessions = new Session[MAX_LINES];
		for (int i = 0; i < sessions.length; i++)
		{
			sessions[i] = new Session();
			sessions[i].lineName = "line - " + i;

		}
	}

	public void hangupAllCalls(PortSipSdk sdk)
	{

		 for (Session session: sessions)
		{
			if (session.sessionID > Session.INVALID_SESSION_ID)
			{
				sdk.hangUp(session.sessionID);
			}
		}
	}

	public boolean hasActiveSession()
	{

		for(Session session: sessions)
		{
			if (session.sessionID > Session.INVALID_SESSION_ID)
			{
				return true;
			}
		}

		return false;
	}

	public Session findSessionBySessionID(long SessionID)
	{
		for(Session session :sessions)
		{
			if (session.sessionID == SessionID)
			{
				return session;
			}
		}
		return null;
	}

	public Session findIdleSession()
	{
		for(Session session :sessions)
		{
			if (session.isIdle())
			{
				return session;
			}
		}
		return null;
	}

	public Session getCurrentSession()
	{
		if (CurrentLine >= 0 && CurrentLine <= sessions.length)
		{

			return sessions[CurrentLine];

		}
		return null;
	}

	public Session findSessionByIndex(int index)
	{
		if (index >= 0 && index <= sessions.length)
		{

			return sessions[index];

		}
		return null;
	}
    public void addActiveSessionToConfrence(PortSipSdk sdk)
    {
        for (Session session : sessions)
        {
            if(session.state == Session.CALL_STATE_FLAG.CONNECTED)
            {
                sdk.joinToConference(session.sessionID);
                sdk.sendVideo(session.sessionID, true);
            }
        }
    }

	public void setRemoteVideoWindow(PortSipSdk sdk,long sessionid,PortSIPVideoRenderer renderer){
		sdk.setConferenceVideoWindow(null);
		for (Session session : sessions)
		{
			if(session.state == Session.CALL_STATE_FLAG.CONNECTED&&sessionid!=session.sessionID)
			{
				sdk.setRemoteVideoWindow(session.sessionID,null);
			}
		}
		sdk.setRemoteVideoWindow(sessionid,renderer);
	}

	public void setConferenceVideoWindow(PortSipSdk sdk,PortSIPVideoRenderer renderer){
		for (Session session : sessions)
		{
			if(session.state == Session.CALL_STATE_FLAG.CONNECTED)
			{
				sdk.setRemoteVideoWindow(session.sessionID,null);
			}
		}
		sdk.setConferenceVideoWindow(renderer);
	}
	public void resetAll()
	{
		for(Session session :sessions)
		{
			session.reset();
		}
	}

	public Session findIncomingCall()
	{
		for(Session session :sessions)
		{
			if (session.sessionID != Session.INVALID_SESSION_ID&&session.state== Session.CALL_STATE_FLAG.INCOMING)
			{
				return session;
			}
		}

		return null;
	}

}


