package com.jakemcginty.voyagr.summary.list;

public class SummaryItem {
	public String title, content, desc;
	public int resId;

	public SummaryItem() { }

	public SummaryItem(String title, String content, String desc, int resId) {
		this.title = title;
		this.content = content;
		this.desc = desc;
		this.resId = resId;
	}

	@Override
	public String toString() {
		return this.content;
	}
}
