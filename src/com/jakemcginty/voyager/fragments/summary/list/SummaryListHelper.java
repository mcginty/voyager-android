package com.jakemcginty.voyager.fragments.summary.list;

import java.io.InputStream;
import java.util.List;

import android.content.Context;
import android.widget.ListView;

import com.jakemcginty.voyager.R;

public class SummaryListHelper {
	public static void initializeSummaryList(Context context, ListView lv) {
		SummaryItemParser summaryParser = new SummaryItemParser();
		InputStream inputStream = context.getResources().openRawResource(R.raw.summary_items);
		summaryParser.parse(inputStream);
		List<SummaryItem> summaryList = summaryParser.getList();
		SummaryItemArrayAdapter adapter = new SummaryItemArrayAdapter(context, R.layout.summary_listitem, summaryList);
		lv.setAdapter(adapter);
	}
}
