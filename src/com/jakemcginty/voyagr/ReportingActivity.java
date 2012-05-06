package com.jakemcginty.voyagr;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jakemcginty.voyagr.preferences.Prefs;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ReportingActivity extends Activity  {
	SharedPreferences settings;
	static final String tag = "ReportingActivity"; // for Log
	private VoyagrService mBoundService;
	private boolean mIsBound = false;
	TextView mLastCheckText, mGPSDebugInfo, mToURLText;
	CheckBox mReportCheck;
	Spinner  mDurationSelect;
	private String postURL;
	long     lastReport = 0L;
	Location lastLocation = null;

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

	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	        mBoundService = ((VoyagrService.LocalBinder)service).getService();

	        // Tell the user about this for our demo.
	        Toast.makeText(ReportingActivity.this, "Local Service connected",
	                Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mBoundService = null;
	        Toast.makeText(ReportingActivity.this, "Local Service disconnected",
	                Toast.LENGTH_SHORT).show();
	    }
	};
	
	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
	    bindService(new Intent(ReportingActivity.this, 
	            VoyagrService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	    }
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reporter);
		
		settings = getSharedPreferences(Prefs.prefsName, 0);
		postURL = settings.getString("postURL", Prefs.defaultPostURL);

		mLastCheckText  = (TextView) findViewById(R.id.lastCheckText);
		mGPSDebugInfo   = (TextView) findViewById(R.id.GPSDebugText);
		mToURLText	    = (TextView) findViewById(R.id.toURL);
		mReportCheck    = (CheckBox) findViewById(R.id.reportLocationCheckbox);
		mDurationSelect = (Spinner)  findViewById(R.id.durationSelect);

		mLastCheckText.post(onEverySecond);
		doBindService();

		mReportCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					mBoundService.startTracking();
				} else {
					mBoundService.stopTracking();
				}
			}
		});
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

				/* Tell our service to start tracking. */
				mBoundService.setTrackingDuration(duration);
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				Log.w(tag, "Nothing was selected for some stupid weird inexplicable reason. Heading to the bomb shelter.");
			}
        });

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
    	Log.d(tag, "Registering receiver with receiverFilter.");
    	lastReport = settings.getLong("lastReport", lastReport);
		registerReceiver(receiver, new IntentFilter(VoyagrService.LOCATION_UPDATE));
		super.onResume();
	}

    @Override
	protected void onPause() {
		/* GPS, as it turns out, consumes battery like crazy */
    	Log.d(tag, "Unregistering receiver...");
    	unregisterReceiver(receiver);
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("postURL", postURL);
		finish();
	}

	private Runnable onEverySecond=new Runnable() {
	    public void run() {
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

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
	}
}
