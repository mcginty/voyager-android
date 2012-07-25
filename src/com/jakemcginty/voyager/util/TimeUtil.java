package com.jakemcginty.voyager.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class TimeUtil {
	public final static String tag = "TimeUtil";
	
	public static String humanTimeDifference(long t1, long t2) {
		if (t1 == 0) return "(waiting)";
		long diff = t2-t1;
		if (diff < 1000) return "<1 second"; else diff = diff / 1000;
		if (diff < 60)   return diff + " second" + (diff>1?"s":"") + " ago"; else diff = diff / 60;
		if (diff < 60)   return diff + " minute" + (diff>1?"s":"") + " ago"; else diff = diff / 60;
		if (diff < 60)   return diff + " hour"   + (diff>1?"s":"") + " ago"; else diff = diff / 24;
		if (diff < 3)    return diff + " day"    + (diff>1?"s":"") + " ago"; return "not recently";
	}
	
	/**
	 * Convert a string of our predictable spinner format into a duration in seconds.
	 * @param s the string to parse
	 * @return duration parsed from string *or* the default duration if can't be parsed.
	 */
	public static long stringPeriodToSecondsDuration(String s) {
		long duration = 5L * 1000L;
		
		if (s.indexOf("fast as possible") > -1) {
			duration = 0L;
		} else {
			Matcher matcher = Pattern.compile("(\\d+) (seconds?|minutes?|hours?)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(s);
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
		return duration;
	}
}
