package com.jakemcginty.voyager.summary;

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
import com.jakemcginty.voyager.preferences.Prefs;
import com.jakemcginty.voyager.summary.list.SummaryItem;
import com.jakemcginty.voyager.summary.list.SummaryItemArrayAdapter;
import com.jakemcginty.voyager.summary.list.SummaryItemParser;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class SummaryFragment extends SherlockFragment {
	
	SharedPreferences settings;
	static final String tag = "LocationFragment"; // for Log
	private VoyagerService mBoundService;
	CheckBox mReportCheck;
	Spinner  mDurationSelect;
	ListView lv;
	ImageView img_map;
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
            if (intent.getAction().equals(VoyagerService.LOCATION_UPDATE)) {
            	lastReport   = intent.getLongExtra("lastReport", lastReport);
            	lastLocation = intent.getParcelableExtra("location");
            }
        }
	}
	BroadcastReceiver receiver = new ReportPostReceiver();

	public static SummaryFragment newInstance() {
		SummaryFragment fragment = new SummaryFragment();

		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.reporter, container, false);

		reportActivity = (ReportingActivity) getActivity();
		settings = reportActivity.getSharedPreferences(Prefs.prefsName, 0);
		postURL = settings.getString("postURL", Prefs.defaultPostURL);
		mDurationSelect = (Spinner)  v.findViewById(R.id.durationSelect);
		
		final SharedPreferences settings = getActivity().getSharedPreferences(Prefs.prefsName, 0);
		String interval = settings.getString("gps_interval", null);
		for (int position=0; position < mDurationSelect.getAdapter().getCount(); position++) {
			String item = (String) mDurationSelect.getAdapter().getItem(position);
			if (item != null && item.equals(interval)) {
				mDurationSelect.setSelection(position);
			}
		}

		SummaryItemParser summaryParser = new SummaryItemParser();
		InputStream inputStream = getResources().openRawResource(R.raw.summary_items);
		summaryParser.parse(inputStream);
		List<SummaryItem> summaryList = summaryParser.getList();
		SummaryItemArrayAdapter adapter = new SummaryItemArrayAdapter(reportActivity, R.layout.summary_listitem, summaryList);
		lv = (ListView) v.findViewById(R.id.summary_list);
		img_map = (ImageView) v.findViewById(R.id.img_map);
		lv.setAdapter(adapter);
		lv.post(onEverySecond);

		/* Attempt to make the domain pretty when presenting it to the user
		try {
			mToURLText.setText("to " + new URL(postURL).getHost());
		} catch (MalformedURLException e) {
			mToURLText.setText("to " + postURL);
		}*/
        mDurationSelect.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String value = mDurationSelect.getAdapter().getItem(position).toString();
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("gps_interval", value);
				editor.commit();
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
				if (mBoundService != null) mBoundService.setTrackingDuration(duration);
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
	    	TextView gps_content = (TextView) lv.getChildAt(0).findViewById(R.id.summary_item_content);
	    	TextView gps_desc = (TextView) lv.getChildAt(0).findViewById(R.id.summary_item_desc);
	    	TextView post_content = (TextView) lv.getChildAt(1).findViewById(R.id.summary_item_content);
	    	TextView post_desc = (TextView) lv.getChildAt(1).findViewById(R.id.summary_item_desc);
	    	if (lastLocation != null) {
	    		SummaryItemArrayAdapter adapter = (SummaryItemArrayAdapter) lv.getAdapter();
	    		
	    		gps_content.setText(String.format("%f, %f", lastLocation.getLatitude(), lastLocation.getLongitude()));
	    		gps_desc.setText(String.format("%.1fm/s, %.1fm above sea, %.1fm accurate", lastLocation.getSpeed(), lastLocation.getAltitude(), lastLocation.getAccuracy()));
	    		//TODO move this to its own "get google static image thingy"
	    		UrlImageViewHelper.setUrlDrawable(img_map, "http://maps.google.com/maps/api/staticmap?center="+lastLocation.getLatitude()+","+lastLocation.getLongitude()+"&zoom=5&markers="+lastLocation.getLatitude()+","+lastLocation.getLongitude()+"&size="+img_map.getWidth()+"x"+img_map.getHeight()+"&sensor=true");
	    	} else {
	    		gps_content.setText("waiting...");
	    		gps_desc.setText(null);
	    	}
	    	
	    	if (lastReport > 0) {
	    		post_desc.setText(String.format("Last successful report was %s", humanTimeDifference(lastReport, new Date().getTime())));
	    	} else {
	    		post_desc.setText("No successful reports yet...");
	    	}
	    	lv.postDelayed(onEverySecond, 1000);
	    }
	};
}
