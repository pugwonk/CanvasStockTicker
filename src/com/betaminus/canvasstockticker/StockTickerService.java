package com.betaminus.canvasstockticker;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class StockTickerService extends Service {
	private static Timer timer = new Timer();
	private Context ctx;

	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void onCreate() {
		super.onCreate();
        Log.i(StockTickerPlugin.LOG_TAG, "Creating service");
		ctx = this;
		startService();
	}

	private void startService() {
        Log.i(StockTickerPlugin.LOG_TAG, "Starting service");
		timer.scheduleAtFixedRate(new mainTask(), 0, 1*60000); // 15*60000); // 15 minute somewhat arbitrary update time
	}

	private class mainTask extends TimerTask {
		public void run() {
	        Log.i(StockTickerPlugin.LOG_TAG, "Service tick");
	        StockTickerPlugin.updateTicker(getApplicationContext());
		}
	}

	public void onDestroy() {
		super.onDestroy();
        Log.i(StockTickerPlugin.LOG_TAG, "Stopping service");
	}
}