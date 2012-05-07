package com.jakemcginty.voyagr.fragments;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.jakemcginty.voyagr.R;
import com.jakemcginty.voyagr.ReportingActivity;
import com.jakemcginty.voyagr.VoyagrService;
import com.jakemcginty.voyagr.preferences.Prefs;

public class LocationFragment extends SherlockFragment {
	
	SharedPreferences settings;
	static final String tag = "LocationFragment"; // for Log
	private VoyagrService mBoundService;
	TextView mLastCheckText, mGPSDebugInfo, mToURLText;
	CheckBox mReportCheck;
	Spinner  mDurationSelect;
	private String postURL;
	long     lastReport = 0L;
	Location lastLocation = null;
	ReportingActivity reportActivity;

	public class ReportPostReceiver extends BroadcastReceiver {
		public ReportPostReceiver() {
			super();
		}

        @Override
        public void onReceive(Context context, Intent intent) {
        	Log.d(tag, "braodcast received with intent action " + intent.getAction());
            if (intent.getAction().equals(VoyagrService.LOCATION_UPDATE)) {
            	lastReport   = intent.getLongExtra("lastReport", lastReport);
            	lastLocation = intent.getParcelableExtra("location");
            }
        }
	}
	BroadcastReceiver receiver = new ReportPostReceiver();

	public static LocationFragment newInstance() {
		LocationFragment fragment = new LocationFragment();

		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.reporter, container, false);

		reportActivity = (ReportingActivity) getActivity();
		settings = reportActivity.getSharedPreferences(Prefs.prefsName, 0);
		postURL = settings.getString("postURL", Prefs.defaultPostURL);
		mLastCheckText  = (TextView) v.findViewById(R.id.lastCheckText);
		mGPSDebugInfo   = (TextView) v.findViewById(R.id.GPSDebugText);
		mToURLText	    = (TextView) v.findViewById(R.id.toURL);
		mDurationSelect = (Spinner)  v.findViewById(R.id.durationSelect);
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
				Log.d(tag,"Item " +position+ " selected with id " +id+ ". Maps to: " +value);
				if (value.indexOf("fast as possible") > -1) {
					duration = 0L;
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
					Log.d(tag, "New duration in milliseconds: " +duration);
				}

				/* Tell our service to start tracking. TODO: uncomment this shit*/
				mBoundService = reportActivity.getmBoundService();
				if (mBoundService != null)
					mBoundService.setTrackingDuration(duration);
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				Log.w(tag, "Nothing was selected for some stupid weird inexplicable reason. Heading to the bomb shelter.");
			}
        });

		return v;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Log.d(tag, "Reporting activity created");
    }
	
	@Override
	public void onResume() {
		super.onResume();
    	lastReport = settings.getLong("lastReport", lastReport);
	}

	private String humanTimeDifference(long t1, long t2) {
		if (t1 == 0) return "(waiting)";
		long diff = t2-t1;
		if (diff < 1000) return "<1 second"; else diff = diff / 1000;
		if (diff < 60)   return diff + " second" + (diff>1?"s":"") + " ago"; else diff = diff / 60;
		if (diff < 60)   return diff + " minute" + (diff>1?"s":"") + " ago"; else diff = diff / 60;
		if (diff < 60)   return diff + " hour"   + (diff>1?"s":"") + " ago"; else diff = diff / 24;
		if (diff < 3)    return diff + " day"    + (diff>1?"s":"") + " ago"; return "not recently";
	}

	private Runnable onEverySecond=new Runnable() {
	    public void run() {
	    	lastReport = reportActivity.getLastReport();
	    	lastLocation = reportActivity.getLastLocation();
	    	if (lastLocation != null) {
		    	String gpsInfo = (new StringBuilder())
		    	  .append("Latitude: ").append(lastLocation.getLatitude())
		    	  .append("\nLongitude: ").append(lastLocation.getLongitude())
		    	  .append("\nAltitude: ").append(lastLocation.getAltitude()).append(" m")
		    	  .append("\nAccuracy: ").append(lastLocation.getAccuracy()).append(" m radius")
		    	  .append("\nSpeed: ").append(lastLocation.getSpeed()).append(" m/s").toString();
		    	mGPSDebugInfo.setText(gpsInfo);
	    	}
	    	mLastCheckText.setText(humanTimeDifference(lastReport, new Date().getTime()));
	    	mLastCheckText.postDelayed(onEverySecond, 1000);
	    }
	};
}
