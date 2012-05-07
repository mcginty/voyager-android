package com.jakemcginty.voyagr.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.jakemcginty.voyagr.R;

public class StatsFragment extends SherlockFragment {
	
	public static StatsFragment newInstance() {
		return new StatsFragment();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.statistics, container, false);
		return v;
	}
}
