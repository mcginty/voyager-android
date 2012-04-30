package com.jakemcginty.voyagr.internet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.jakemcginty.voyagr.VoyagrService;

public class ReportPostService extends IntentService {

	private final String tag = "ReportPostService";
	public ReportPostService() {
		super("ReportPostService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Location location = (Location) intent.getParcelableExtra("location");
		String postURL = intent.getStringExtra("postURL");
		Log.d(tag,"ReportPostService at location " +location.toString()+ " asked to be posted to " +postURL+ ".");
		postLocation(location, postURL);
	}

	private void postLocation(Location location, String postURL) {
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(postURL);

		try {
			/* Create POST data to submit to the voyagr web application. */
			List<NameValuePair> postData = new ArrayList<NameValuePair>(2);
			postData.add(new BasicNameValuePair("timestamp", String.valueOf(location.getTime())));
			postData.add(new BasicNameValuePair("longitude", String.valueOf(location.getLongitude())));
			postData.add(new BasicNameValuePair("latitude",  String.valueOf(location.getLatitude())));
			postData.add(new BasicNameValuePair("altitude",  String.valueOf(location.getAltitude())));
			postData.add(new BasicNameValuePair("accuracy",  String.valueOf(location.getAccuracy())));
			httppost.setEntity(new UrlEncodedFormEntity(postData));
			Log.d(tag, "Submission data created for location " +location.toString()+ ", attempting to execute POST request...");
			HttpResponse response = httpclient.execute(httppost);

			Log.d(tag, "POST data for location "+location.toString()+" succeeded with status " + response.getStatusLine());

			Intent i = new Intent();
			i.setAction(VoyagrService.LOCATION_UPDATE);
			i.putExtra("lastReport", new Date().getTime());
			sendBroadcast(i);
	    } catch (ClientProtocolException e) {
	    	Log.e(tag, "ClientProtocolException: " + e.getMessage());
	    } catch (IOException e) {
	    	Log.e(tag, "IOException: " + e.getMessage());
	    }
	}
}
