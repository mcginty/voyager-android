package com.jakemcginty.voyager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import com.jakemcginty.voyager.R;

public class ReportBroadcastReceiver extends BroadcastReceiver {
	TextView mLastCheckText;
	private final String tag = "ReportBraodcastReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(tag, "Received a location braodcast update.");
		if (intent.getAction() == "voyagr.intent.LOCATION_UPDATE") {
			Intent i = new Intent(context, ReportingActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		}
	}

}
