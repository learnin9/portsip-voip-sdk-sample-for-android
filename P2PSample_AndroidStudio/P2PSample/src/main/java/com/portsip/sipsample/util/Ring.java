package com.portsip.sipsample.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;

public class Ring
{
	private static int TONE_RELATIVE_VOLUME = 70;
	private ToneGenerator mRingbackPlayer;
	protected Ringtone mRingtonePlayer;
	int ringRef = 0;
	private Context mContext;
	private static Ring single = null;
	private int savedMode = AudioManager.MODE_INVALID;
	AudioManager audioManager;
	public static Ring getInstance(Context context)
	{
		if (single == null)
		{
			single = new Ring(context);
		}
		return single;
	}

	private Ring(Context context)
	{
		mContext = context;
		audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	}

	public boolean stop()
	{
		stopRingBackTone();
		stopRingTone();
		return true;
	}


	public void startRingTone()
	{
		if (mRingtonePlayer != null && mRingtonePlayer.isPlaying())
		{
			ringRef++;
			return;
		}

		if (mRingtonePlayer == null && mContext != null)
		{
			mRingtonePlayer = RingtoneManager.getRingtone(mContext, android.provider.Settings.System.DEFAULT_RINGTONE_URI);
		}

		savedMode =audioManager.getMode();
		audioManager.setMode(AudioManager.MODE_RINGTONE);

		if (mRingtonePlayer != null)
		{
			synchronized (mRingtonePlayer){
				ringRef++;
				mRingtonePlayer.play();
			}
		}
	}

	public void stopRingTone()
	{
		if (mRingtonePlayer != null)
		{
			synchronized(mRingtonePlayer){

				if (--ringRef <= 0)
				{
					audioManager.setMode(savedMode);

					mRingtonePlayer.stop();
					mRingtonePlayer = null;
				}
			}
		}
	}

	public void startRingBackTone() {
		if (mRingbackPlayer == null) {
			try {
				mRingbackPlayer = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, TONE_RELATIVE_VOLUME);
			} catch (RuntimeException e) {
				mRingbackPlayer = null;
			}
		}

		if(mRingbackPlayer != null){
			synchronized(mRingbackPlayer){
				mRingbackPlayer.startTone(ToneGenerator.TONE_SUP_RINGTONE);
			}
		}
	}

	public void stopRingBackTone()
	{
		if (mRingbackPlayer != null)
		{
			synchronized(mRingbackPlayer){
				mRingbackPlayer.stopTone();
				mRingbackPlayer = null;
			}
		}
	}

}


