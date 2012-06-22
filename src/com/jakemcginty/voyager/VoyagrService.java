package com.jakemcginty.voyager;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.jakemcginty.voyager.R;
import com.jakemcginty.voyager.internet.ReportPostService;
import com.jakemcginty.voyager.preferences.Prefs;

public class VoyagrService extends Service implements LocationListener {

	public static final String LOCATION_UPDATE = Prefs.broadcastAction;
	boolean reportGPS=true;
	LocationManager lm;
	private final String tag = "VoyagrService";
	private String postURL = Prefs.defaultPostURL;
	private long duration = Prefs.defaultDuration;
	
	public VoyagrService() {
		super();
	}
	
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
	public class LocalBinder extends Binder {
        VoyagrService getService() {
            return VoyagrService.this;
        }
    }

    @Override
    public void onCreate() {
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        startTracking();
        Log.d(tag, "VoyagrService created.");
    }

    @Override
    public void onDestroy() {
    	stopTracking();
    	super.onDestroy();
        Log.d(tag, "VoyagrService destroyed.");
    }

	@Override
	public void onLocationChanged(Location location) {
		/* If they don't want us to report, then we will skip the update. */
		Log.d(tag, "Location Changed: " + location.toString());

		Intent intent = new Intent(this, ReportPostService.class);
		intent.putExtra("location", location);
		intent.putExtra("postURL", postURL);
		startService(intent);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(tag, "Disabled");

		/* bring up the GPS settings */
		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(tag, "Enabled");
		Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		/* This is called when the GPS status alters */
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			Log.v(tag, "Status Changed: Out of Service");
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.v(tag, "Status Changed: Temporarily Unavailable");
			break;
		case LocationProvider.AVAILABLE:
			Log.v(tag, "Status Changed: Available");
			break;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	public synchronized void setTrackingDuration(long duration) {
		this.duration = duration;
		startTracking();
	}

	public synchronized void startTracking() {
		lm.removeUpdates(VoyagrService.this);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, duration, 0f, VoyagrService.this);

        // Display a notification about us starting.  We put an icon in the status bar.
        int icon = R.drawable.ic_stat_voyagr;
        CharSequence voyagrTitle = "Voyagr";
        CharSequence voyagrText  = "Currently reporting your position.";
        Notification notification = new Notification(icon, voyagrText, System.currentTimeMillis());
        Intent notificationIntent = new Intent(this,ReportingActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(getApplicationContext(), voyagrTitle, voyagrText, contentIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        startForeground(1, notification);
	}

	public synchronized void stopTracking() {
		lm.removeUpdates(this);
		stopForeground(true);
	}
}
