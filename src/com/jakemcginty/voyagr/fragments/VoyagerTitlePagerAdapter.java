package com.jakemcginty.voyagr.fragments;

import com.viewpagerindicator.TitleProvider;

import android.support.v4.app.FragmentManager;

public class VoyagerTitlePagerAdapter extends VoyagerPagerAdapter implements TitleProvider {

	public VoyagerTitlePagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public String getTitle(int position) {
		return VoyagerPagerAdapter.CONTENT[position % CONTENT.length];
	}
}
