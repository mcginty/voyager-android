package com.jakemcginty.voyager.fragments.summary;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.jakemcginty.voyager.R;
import com.jakemcginty.voyager.ReportingActivity;
import com.jakemcginty.voyager.VoyagerService;
import com.jakemcginty.voyager.fragments.summary.list.SummaryItem;
import com.jakemcginty.voyager.fragments.summary.list.SummaryItemArrayAdapter;
import com.jakemcginty.voyager.fragments.summary.list.SummaryItemParser;
import com.jakemcginty.voyager.fragments.summary.list.SummaryListHelper;
import com.jakemcginty.voyager.preferences.Prefs;
import com.jakemcginty.voyager.util.TimeUtil;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class SummaryFragment extends SherlockFragment {
	
	SharedPreferences settings;
	static final String tag = "LocationFragment"; // for Log
	private VoyagerService mBoundService;
	CheckBox mReportCheck;
	Spinner  mDurationSelect;
	ListView mSummaryList;
	ImageView mImgMap;
	private String postURL;
	long     lastReport = 0L;
	Location lastLocation = null;
	ReportingActivity mActivity;

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

	/**
	 * Self-explanatorilarily gives back a new instance of this.
	 * @return a new SummaryFragment instance
	 */
	public static SummaryFragment newInstance() {
		return new SummaryFragment();
	}
	
	/**
	 * Main listener for delegating summary ListItem clicks to handle their own tasks.
	 */
	private ListView.OnItemClickListener summaryItemClickedListener = new ListView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			//TODO update this integer with a constant
			if (position == 2) mDurationSelect.performClick();
		}
		
	};
	
	/**
	 * Whenever a new duration (GPS/POST period) is selected, we need to update our shit.
	 */
	private OnItemSelectedListener durationSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			// value = the text for the item they chose on screen
			String value = mDurationSelect.getAdapter().getItem(position).toString();
			Log.d(tag,"Item " +position+ " selected with id " +id+ ". Maps to: " +value);
			// persist in preferences for the next time we start
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("gps_interval", value);
			editor.commit();
			// convert the selection to a time we can actually use, and tell the service to update its duration accordingly
			long duration = TimeUtil.stringPeriodToSecondsDuration(value);
			mBoundService = mActivity.getmBoundService();
			if (mBoundService != null) mBoundService.setTrackingDuration(duration);
		}
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			Log.d(tag, "Nothing was selected. Heading to the bomb shelter.");
		}
    };
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// View setup
		View v = inflater.inflate(R.layout.summary, container, false);
		mActivity = (ReportingActivity) getActivity();
		mDurationSelect = (Spinner)  v.findViewById(R.id.durationSelect);
		mSummaryList = (ListView) v.findViewById(R.id.summary_list);
		mImgMap = (ImageView) v.findViewById(R.id.img_map);
		// Settings
		settings = mActivity.getSharedPreferences(Prefs.prefsName, 0);
		postURL = settings.getString("postURL", Prefs.defaultPostURL);
		// Summary List preparation
		SummaryListHelper.initializeSummaryList(mActivity, mSummaryList);
		mSummaryList.setOnItemClickListener(summaryItemClickedListener);
		mSummaryList.post(redraw);
		// Duration spinner preparation
        mDurationSelect.setOnItemSelectedListener(durationSelectedListener);

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
		String interval = settings.getString("gps_interval", null);
		for (int position=0; position < mDurationSelect.getAdapter().getCount(); position++) {
			String item = mDurationSelect.getAdapter().getItem(position).toString();
			if (item != null && item.equals(interval)) {
				mDurationSelect.setSelection(position);
			}
		}

	}

	private Runnable redraw = new Runnable() {
	    public void run() {
	    	lastReport = mActivity.getLastReport();
	    	lastLocation = mActivity.getLastLocation();
	    	TextView gps_content = (TextView) mSummaryList.getChildAt(0).findViewById(R.id.summary_item_content);
	    	TextView gps_desc = (TextView) mSummaryList.getChildAt(0).findViewById(R.id.summary_item_desc);
	    	TextView post_content = (TextView) mSummaryList.getChildAt(1).findViewById(R.id.summary_item_content);
	    	TextView post_desc = (TextView) mSummaryList.getChildAt(1).findViewById(R.id.summary_item_desc);
	    	TextView freq_content = (TextView) mSummaryList.getChildAt(2).findViewById(R.id.summary_item_content);
	    	if (lastLocation != null) {
	    		SummaryItemArrayAdapter adapter = (SummaryItemArrayAdapter) mSummaryList.getAdapter();
	    		
	    		gps_content.setText(String.format("%f, %f", lastLocation.getLatitude(), lastLocation.getLongitude()));
	    		gps_desc.setText(String.format("%.1fm/s, %.1fm above sea, %.1fm accurate", lastLocation.getSpeed(), lastLocation.getAltitude(), lastLocation.getAccuracy()));
	    		//TODO move this to its own "get google static image thingy"
	    		UrlImageViewHelper.setUrlDrawable(mImgMap, "http://maps.google.com/maps/api/staticmap?center="+lastLocation.getLatitude()+","+lastLocation.getLongitude()+"&zoom=5&markers="+lastLocation.getLatitude()+","+lastLocation.getLongitude()+"&size="+mImgMap.getWidth()+"x"+mImgMap.getHeight()+"&sensor=true");
	    	} else {
	    		gps_content.setText("waiting...");
	    		gps_desc.setText(null);
	    	}
	    	
	    	if (lastReport > 0) {
	    		post_desc.setText(String.format("Last successful report was %s", TimeUtil.humanTimeDifference(lastReport, new Date().getTime())));
	    	} else {
	    		post_desc.setText("No successful reports yet...");
	    	}
	    	
	    	freq_content.setText(mDurationSelect.getSelectedItem().toString());
	    	mSummaryList.postDelayed(redraw, 1000);
	    }
	};
}
