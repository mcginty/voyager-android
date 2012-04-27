package com.jakemcginty.voyagr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ReportingService extends Service implements LocationListener {

	LocationManager lm;
	private final String tag = "ReportingService";
	private String postURL="http://jake.su/report";
	
	public ReportingService() {
		super();
	}

	@Override
	public void onLocationChanged(Location location) {
		/* If they don't want us to report, then we will skip the update. */
		if (!mReportCheck.isChecked()) return;

		Log.v(tag, "Location Changed");

		StringBuilder sb = new StringBuilder(512);

		/* display some of the data in the TextView */

		sb.append("Londitude: ");
		sb.append(location.getLongitude());
		sb.append('\n');

		sb.append("Latitude: ");
		sb.append(location.getLatitude());
		sb.append('\n');

		sb.append("Altitiude: ");
		sb.append(location.getAltitude());
		sb.append('\n');

		sb.append("Accuracy: ");
		sb.append(location.getAccuracy());
		sb.append('\n');

		sb.append("Timestamp: ");
		sb.append(location.getTime());
		sb.append('\n');

		//mGPSDebugInfo.setText(sb.toString());

		final Location locationDat = location;

		new Thread(new Runnable() {
			Location loc = locationDat;
			@Override
	        public void run() {
				Log.d(tag, "trying to post data to http"); 
			    HttpClient httpclient = new DefaultHttpClient();
			    HttpPost httppost = new HttpPost(postURL);

				try {
					List<NameValuePair> postData = new ArrayList<NameValuePair>(2);
					postData.add(new BasicNameValuePair("timestamp", String.valueOf(loc.getTime())));
					postData.add(new BasicNameValuePair("longitude", String.valueOf(loc.getLongitude())));
					postData.add(new BasicNameValuePair("latitude", String.valueOf(loc.getLatitude())));
					postData.add(new BasicNameValuePair("altitude", String.valueOf(loc.getAltitude())));
					postData.add(new BasicNameValuePair("accuracy", String.valueOf(loc.getAccuracy())));
					httppost.setEntity(new UrlEncodedFormEntity(postData));
					Log.d(tag, "post data created, trying to execute");
					httpclient.execute(httppost);

					Log.d(tag, "http data executed and posted properly");
					lastReport = new Date().getTime();
			    } catch (ClientProtocolException e) {
			    	Log.e(tag, "ClientProtocolException: " + e.getMessage());
			    } catch (IOException e) {
			    	Log.e(tag, "IOException: " + e.getMessage());
			    }
	        }
		}).start();
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.v(tag, "Disabled");

		/* bring up the GPS settings */
		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.v(tag, "Enabled");
		Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		/* This is called when the GPS status alters */
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			Log.v(tag, "Status Changed: Out of Service");
			Toast.makeText(this, "Status Changed: Out of Service",
					Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.v(tag, "Status Changed: Temporarily Unavailable");
			Toast.makeText(this, "Status Changed: Temporarily Unavailable",
					Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.AVAILABLE:
			Log.v(tag, "Status Changed: Available");
			Toast.makeText(this, "Status Changed: Available",
					Toast.LENGTH_SHORT).show();
			break;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
