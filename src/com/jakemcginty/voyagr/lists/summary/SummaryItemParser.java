package com.jakemcginty.voyagr.lists.summary;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.util.Log;

import com.jakemcginty.voyagr.R;

public class SummaryItemParser {
	private static final String tag = "SummaryItemParser";
	
	private DocumentBuilderFactory factory;
	private DocumentBuilder builder;
	private final List<SummaryItem> list;

	public SummaryItemParser() {
		this.list = new ArrayList<SummaryItem>();
	}
	
	private String getNodeValue(NamedNodeMap map, String key) {
		String nodeValue = null;
		Node node = map.getNamedItem(key);
		if (node != null) {
			nodeValue = node.getNodeValue();
		}
		return nodeValue;
	}

	public List<SummaryItem> getList() {
		return this.list;
	}
	
	/**
	 * Parse XML file containing body part X/Y/Description
	 * 
	 * @param inStream
	 */
	public void parse(InputStream inStream) {
		try {
			// TODO: after we must do a cache of this XML!!!!
			this.factory = DocumentBuilderFactory.newInstance();
			this.builder = this.factory.newDocumentBuilder();
			this.builder.isValidating();
			Document doc = this.builder.parse(inStream, null);

			doc.getDocumentElement().normalize();

			NodeList summaryItem = doc.getElementsByTagName("summary_item");
			final int length = summaryItem.getLength();

			for (int i = 0; i < length; i++) {
				final NamedNodeMap attr = summaryItem.item(i).getAttributes();
				final String title = getNodeValue(attr, "title");
				final String content = getNodeValue(attr, "content");
				final String desc = getNodeValue(attr, "desc");
				final String iconStr = getNodeValue(attr, "icon");
				int icon = 0;
				try {
				    Class res = R.drawable.class;
				    Field field = res.getField(iconStr);
				    icon = field.getInt(null);
				}
				catch (Exception e) {
				    Log.e("MyTag", "Failure to get drawable id.", e);
				}				
				SummaryItem item = new SummaryItem(title, content, desc, icon);
				
				// Add to list
				this.list.add(item);
				
				Log.d(tag, item.toString());
			}
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
}
