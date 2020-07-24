package com.portsip.sipsample.util;


public class Session
{
	public static int INVALID_SESSION_ID = -1;
	public long sessionID;

	public String remote;
	public String displayName;
	public boolean hasVideo;
	public boolean bHold;
	public boolean bMute;
	public String lineName;
	public CALL_STATE_FLAG state;

	public Session()
	{
		remote = null;
		displayName = null;
		hasVideo = false;
		sessionID = INVALID_SESSION_ID;
		state = CALL_STATE_FLAG.CLOSED;
	}

	public boolean isIdle()
	{
		return state == CALL_STATE_FLAG.FAILED || state == CALL_STATE_FLAG.CLOSED;
	}

	public void reset()
	{
		remote = null;
		displayName = null;
		hasVideo = false;
		sessionID = INVALID_SESSION_ID;
		state = CALL_STATE_FLAG.CLOSED;
	}

	public enum CALL_STATE_FLAG
	{
		INCOMING,
		TRYING ,
		CONNECTED,
		FAILED,
		CLOSED,
	}
}


