package com.jakemcginty.voyagr.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class VoyagerPagerAdapter extends FragmentPagerAdapter {
	protected static final String[] CONTENT = new String[] { "Current Trip", "Stats" };
	private int mCount = CONTENT.length;

	public VoyagerPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int num) {
		switch (num) {
		case 0: return LocationFragment.newInstance();
		case 1: return StatsFragment.newInstance();
		default: return LocationFragment.newInstance();
		}
	}

	@Override
	public int getCount() {
		return mCount;
	}
	
	public void setCount(int count) {
		if (count > 0 && count <= 10) {
			mCount = count;
			notifyDataSetChanged();
		}
	}
}
