package com.jakemcginty.voyagr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ReportingActivity extends Activity  {

	static final String tag = "Reporter"; // for Log

	private String postURL="http://10.0.2.2:8081/report";
	TextView mLastCheckText, mGPSDebugInfo, mToURLText;
	CheckBox mReportCheck;
	Spinner  mDurationSelect;
	long     lastReport = 0L;

	private String humanTimeDifference(long t1, long t2) {
		long diff = t2-t1;
		if (diff < 1000) return "<1 second"; else diff = diff / 1000;
		if (diff < 60) return diff + " second" + (diff>1?"s":"") + " ago"; else diff = diff / 60;
		if (diff < 60) return diff + " minute" + (diff>1?"s":"") + " ago"; else diff = diff / 60;
		if (diff < 60) return diff + " hour"   + (diff>1?"s":"") + " ago"; else diff = diff / 24;
		if (diff < 3) return diff + " day"    + (diff>1?"s":"") + " ago";
		return "not recently";
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reporter);

		mLastCheckText  = (TextView) findViewById(R.id.lastCheckText);
		mGPSDebugInfo   = (TextView) findViewById(R.id.GPSDebugText);
		mToURLText	    = (TextView) findViewById(R.id.toURL);
		mReportCheck    = (CheckBox) findViewById(R.id.reportLocationCheckbox);
		mDurationSelect = (Spinner)  findViewById(R.id.durationSelect);

		mLastCheckText.post(onEverySecond);

		/* Attempt to make the domain pretty when presenting it to the user */
		try {
			mToURLText.setText("to " + new URL(postURL).getHost());
		} catch (MalformedURLException e) {
			mToURLText.setText("to " + postURL);
		}
        mDurationSelect.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String value = mDurationSelect.getAdapter().getItem(position).toString();
				long duration = 5L * 1000L; // default duration in milliseconds if we can't parse for some reason
				float dist = 100f;
				Log.d(tag,"Item " + position + " selected with id " + id + ". Maps to: " + value);
				if (value.indexOf("fast as possible") > -1) {
					duration = 0L;
					dist = 0f;
				} else {
					Matcher matcher = Pattern.compile("(\\d+) (seconds?|minutes?|hours?)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(value);
					matcher.find();
					if (matcher.matches() && matcher.groupCount() == 2) { // we need two groups otherwise this is not the pattern we're looking for
						String timeUnit = matcher.group(2);
						Log.d(tag, "Time unit: "+timeUnit);
						if (timeUnit.indexOf("second") > -1)
							duration = Long.valueOf(matcher.group(1)) * 1000L;
						else if (timeUnit.indexOf("minute") > -1)
							duration = Long.valueOf(matcher.group(1)) * 60L * 1000L;
						else if (timeUnit.indexOf("hour") > -1)
							duration = Long.valueOf(matcher.group(1)) * 60L * 60L * 1000L;
						// else duration will remain the default duration
					}
					Log.d(tag, "New duration in milliseconds: " + duration);
				}

				/* Reset LocationManager update requests with the new interval time. */
				lm.removeUpdates(ReportingActivity.this);
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, duration, dist, ReportingActivity.this);
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				Log.w(tag, "Nothing was selected for some stupid weird inexplicable reason. Heading to the bomb shelter.");
			}
        });
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        Log.d(tag, "Reporting activity created");
    }

    @Override
	protected void onResume() {
		/*
		 * onResume is is always called after onStart, even if the app hasn't been
		 * paused
		 *
		 * add location listener and request updates every 1000 ms or 10 meters
		 */
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);
		super.onResume();
	}

    @Override
	protected void onPause() {
		/* GPS, as it turns out, consumes battery like crazy */
		lm.removeUpdates(this);
		super.onPause();
	}



	@Override
	protected void onStop() {
		/* may as well just finish since saving the state is not important for this toy app */
		finish();
		super.onStop();
	}

	private Runnable onEverySecond=new Runnable() {
	    public void run() {
	    	mLastCheckText.setText(humanTimeDifference(lastReport, new Date().getTime()));
	    	mLastCheckText.postDelayed(onEverySecond, 1000);
	    }
	};
}
