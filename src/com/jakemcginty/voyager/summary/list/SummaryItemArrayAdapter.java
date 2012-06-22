package com.jakemcginty.voyager.summary.list;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jakemcginty.voyager.R;

public class SummaryItemArrayAdapter extends ArrayAdapter<SummaryItem> {
	private static final String tag = "SummaryItemArrayAdapter";
	private Context context;
	private ImageView icon;
	private TextView title, content, desc;
	private List<SummaryItem> countries = new ArrayList<SummaryItem>();

	public SummaryItemArrayAdapter(Context context, int textViewResourceId,
			List<SummaryItem> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.countries = objects;
	}

	public int getCount() {
		return this.countries.size();
	}

	public SummaryItem getItem(int index) {
		return this.countries.get(index);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			// ROW INFLATION
			Log.d(tag, "Starting XML Row Inflation ... ");
			LayoutInflater inflater = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.summary_listitem, parent, false);
			Log.d(tag, "Successfully completed XML Row Inflation!");
		}

		// Get item
		SummaryItem summary_item = getItem(position);
		
		// Get reference to ImageView 
		icon = (ImageView) row.findViewById(R.id.summary_item_icon);
		title = (TextView) row.findViewById(R.id.summary_item_title);
		content = (TextView) row.findViewById(R.id.summary_item_content);
		desc = (TextView) row.findViewById(R.id.summary_item_desc);

		//Set country name
		title.setText(summary_item.title);
		content.setText(summary_item.content);
		icon.setImageResource(summary_item.resId);
		
		// Set country abbreviation
		desc.setText(summary_item.desc);
		return row;
	}
}