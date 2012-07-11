package com.jakemcginty.voyager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.jakemcginty.voyager.R;
import com.jakemcginty.voyager.fragments.VoyagerPagerAdapter;
import com.jakemcginty.voyager.fragments.VoyagerTitlePagerAdapter;
import com.jakemcginty.voyager.preferences.Prefs;
import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TitlePageIndicator;

public class ReportingActivity extends SherlockFragmentActivity  {
	SharedPreferences settings;
	static final String tag = "ReportingActivity"; // for Log
	private VoyagerService mBoundService;
	public VoyagerService getmBoundService() {
		return mBoundService;
	}
	boolean gpsActive;
	VoyagerPagerAdapter mAdapter;
	ViewPager mPager;
	PageIndicator mIndicator;
	ActionBar ab;
	private boolean mIsBound = false;
	TextView mLastCheckText, mGPSDebugInfo, mToURLText;
	CheckBox mReportCheck;
	Spinner  mDurationSelect;
	private String postURL;
	long     lastReport = 0L;
	public long getLastReport() {
		return lastReport;
	}

	Location lastLocation = null;

	public Location getLastLocation() {
		return lastLocation;
	}

	public class ReportPostReceiver extends BroadcastReceiver {
		public ReportPostReceiver() {
			super();
		}

        @Override
        public void onReceive(Context context, Intent intent) {
        	Log.d(tag, "braodcast received with intent action " + intent.getAction());
            if (intent.getAction().equals(VoyagerService.LOCATION_UPDATE)) {
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
	        mBoundService = ((VoyagerService.LocalBinder)service).getService();
	        gpsActive = true;

	        // Tell the user about this for our demo.
	        Log.d(tag, "Local Service connected");
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mBoundService = null;
	        gpsActive = false;
	        Log.d(tag, "Local Service disconnected");
	    }
	};

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
	    bindService(new Intent(ReportingActivity.this, 
	            VoyagerService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.root);
		settings = getSharedPreferences(Prefs.prefsName, 0);
		postURL = settings.getString("postURL", Prefs.defaultPostURL);

		mAdapter = new VoyagerTitlePagerAdapter(getSupportFragmentManager());
		
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);
		
		mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
		mIndicator.setViewPager(mPager);
		
		ab = getSupportActionBar();
		ab.setSplitBackgroundDrawable(new GradientDrawable(Orientation.TOP_BOTTOM, new int[]{0x77dddddd, 0x77ffffff}));
		ab.setBackgroundDrawable(new GradientDrawable(Orientation.TOP_BOTTOM, new int[]{0xff333333, 0xff222222}));
		ab.setHomeButtonEnabled(true);
		doBindService();

        Log.d(tag, "Reporting activity created");
    }

	/*
	 * onResume is is always called after onStart, even if the app hasn't been
	 * paused
	 *
	 */
    @Override
	protected void onResume() {
    	Log.d(tag, "Registering receiver with receiverFilter.");
		registerReceiver(receiver, new IntentFilter(VoyagerService.LOCATION_UPDATE));
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

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.actionbar, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int type, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_gps_toggle:
			if (gpsActive) {
				mBoundService.stopTracking();
				item.setIcon(R.drawable.location_off);
				gpsActive = false;
			}
			else {
				mBoundService.startTracking();
				item.setIcon(R.drawable.location_found);
				gpsActive = true;
			}
			return true;
		case R.id.menu_new_voyage:
			return true;
		case R.id.menu_share_voyage:
			return true;
		default:
            return super.onOptionsItemSelected(item);
		}
	}
}
