package com.almalence.opencam;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;

public class AlarmReceiver extends BroadcastReceiver
{
	private static AlarmManager		alarmMgr;
	private static PendingIntent	alarmIntent;

	private long					pauseBetweenShots	= 0;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.e("asd", "onReceive");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		Log.e("TTT", photoTimeLapseActive + "");
		Log.e("TTT", photoTimeLapseIsRunning + "");
		
		if (!photoTimeLapseActive || !photoTimeLapseIsRunning)
		{
			return;
		}

		try
		{
			if (MainScreen.getInstance().getCameraController().getCamera() == null)
			{
				Intent dialogIntent = new Intent(context, MainScreen.class);
				dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(dialogIntent);
			}
		} catch (NullPointerException e)
		{
			Intent dialogIntent = new Intent(context, MainScreen.class);
			dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(dialogIntent);
		}

		pauseBetweenShots = prefs.getLong(MainScreen.sPhotoTimeLapseCaptureIntervalPref, -1);
		Log.e("TTT", pauseBetweenShots + "");
		if (pauseBetweenShots == -1)
		{
			return;
		}

		CountDownTimer timer = new CountDownTimer(6000, 6000)
		{
			public void onTick(long millisUntilFinished)
			{
			}

			public void onFinish()
			{

				Log.e("TTT", PluginManager.getInstance().getActiveMode().modeID);
				
				// Если выбра один из наших модов phototTmeLapse включен
				if (PluginManager.getInstance().getActiveMode().modeID.equals("single")
						|| PluginManager.getInstance().getActiveMode().modeID.equals("hdrmode")
						|| PluginManager.getInstance().getActiveMode().modeID.equals("nightmode"))
				{
					PluginManager.getInstance().onShutterClickNotUser();

					setNewAlarm(pauseBetweenShots * 1000 - 6000);
				}
			}
		};
		timer.start();
	}

	public void setNewAlarm(long time)
	{
		AlarmReceiver alarmReceiver = new AlarmReceiver();
		alarmReceiver.setAlarm(MainScreen.getInstance(), time);
		ComponentName receiver = new ComponentName(MainScreen.getInstance(), AlarmReceiver.class);
		PackageManager pm = MainScreen.getInstance().getPackageManager();

		pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	@SuppressLint("NewApi")
	public void setAlarm(Context context, long intervalMillis)
	{
		alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("flag_alarm", true);
		alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

		if (Build.VERSION.SDK_INT < 19)
		{
			alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis, alarmIntent);
		} else
		{
			alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis, alarmIntent);
		}
	}

	public void setRepeatingAlarm(Context context, long intervalMillis)
	{
		alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (Build.VERSION.SDK_INT < 19)
		{
			alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000,
					System.currentTimeMillis() + intervalMillis, alarmIntent);
		} else
		{
			alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000,
					System.currentTimeMillis() + intervalMillis, alarmIntent);
		}
	}

	public static void cancelAlarm(Context context)
	{
		if (alarmMgr != null && alarmIntent != null)
		{
			alarmMgr.cancel(alarmIntent);
		}
	}
}
