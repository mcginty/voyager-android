package com.jakemcginty.voyager.fragments.adapters;

import com.jakemcginty.voyager.fragments.stats.StatsFragment;
import com.jakemcginty.voyager.fragments.summary.SummaryFragment;

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
		case 0: return SummaryFragment.newInstance();
		case 1: return StatsFragment.newInstance();
		default: return SummaryFragment.newInstance();
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
