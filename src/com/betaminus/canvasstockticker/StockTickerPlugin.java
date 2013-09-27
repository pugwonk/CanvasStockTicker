package com.betaminus.canvasstockticker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import java.util.Arrays;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import com.betaminus.phonepowersource.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin;
import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin.ImagePluginDefinition;

public class StockTickerPlugin extends PebbleCanvasPlugin {
	public static final String LOG_TAG = "CANV_TICKER";

	public static final int TICKER_TEXT_ID = 1; // Needs to be unique only
												// within
	// this plugin package
	public static final int TICKER_DAYCHART_ID = 2;

	private static final String[] MASKS = { "%T", "%S", "%P" };
	private static final int MASK_TIME = 0;
	private static final int MASK_TICKER = 1;
	private static final int MASK_PRICE = 2;

	private static class StockInfo {
		String time;
		// String ticker;
		double price;
	}

	// private static StockInfo current_state = new StockInfo();

	static Map<String, StockInfo> stock_list = new HashMap<String, StockInfo>();

	// send plugin metadata to Canvas when requested
	@Override
	protected ArrayList<PluginDefinition> get_plugin_definitions(Context context) {
		Log.i(LOG_TAG, "get_plugin_definitions");

		// create a list of plugins provided by this app
		ArrayList<PluginDefinition> plugins = new ArrayList<PluginDefinition>();

		// now playing (text)
		TextPluginDefinition tplug = new TextPluginDefinition();
		tplug.id = TICKER_TEXT_ID;
		tplug.name = context.getString(R.string.plugin_name);
		tplug.format_mask_descriptions = new ArrayList<String>(
				Arrays.asList(context.getResources().getStringArray(
						R.array.format_mask_descs)));
		// populate example content for each field (optional) to be display in
		// the format mask editor
		ArrayList<String> examples = new ArrayList<String>();
		examples.add("10:35");
		examples.add("MSFT");
		examples.add("30.52");
		tplug.params_description = "Ticker symbol";
		tplug.format_mask_examples = examples;
		tplug.format_masks = new ArrayList<String>(Arrays.asList(MASKS));
		tplug.default_format_string = "%S: %P";
		plugins.add(tplug);

		// chart
		ImagePluginDefinition iplug = new ImagePluginDefinition();
		iplug.id = TICKER_DAYCHART_ID;
		iplug.name = context.getString(R.string.plugin_name);
		iplug.params_description = "Ticker symbol";
		plugins.add(iplug);

		return plugins;
	}

	// send current text values to canvas when requested
	@Override
	protected String get_format_mask_value(int def_id, String format_mask,
			Context context, String param) {

		Log.i(LOG_TAG, "get_format_mask_value def_id = " + def_id
				+ " format_mask = '" + format_mask + "'");

		if (def_id == TICKER_TEXT_ID) {
			// Service will only get started once, so no great problem
			// re-calling this
			Intent tickerService = new Intent(context, StockTickerService.class);
			context.startService(tickerService);

			if (!stock_list.containsKey(param)) {
				stock_list.put(param, new StockInfo());
				return "...";
			}
			StockInfo current_state = stock_list.get(param);

			// which field to return current value for?
			if (format_mask.equals(MASKS[MASK_TIME])) {
				return current_state.time;
			} else if (format_mask.equals(MASKS[MASK_TICKER])) {
				return param;
			} else if (format_mask.equals(MASKS[MASK_PRICE])) {
				return String.valueOf(current_state.price);
			}
		}
		Log.i(LOG_TAG, "no matching mask found");
		return null;
	}

	// send bitmap value to canvas when requested
	@Override
	protected Bitmap get_bitmap_value(int def_id, Context context, String param) {
		if (def_id == TICKER_DAYCHART_ID) {
			Log.i(LOG_TAG, "Returning day chart");
			// Bitmap bm = BitmapFactory.decodeResource(context.getResources(),
			// R.drawable.canvas_icon);
			StockChart sc = new StockChart(context);
			Bitmap bm = sc.getBitmap();
			return bm;
		}
		Log.i(LOG_TAG, "no matching image mask found");
		return null;
	}

	static Context keepContext;

	public static void updateTicker(Context context) {
		Log.i(LOG_TAG, "Updating ticker");
		AsyncHttpClient client = new AsyncHttpClient();

		keepContext = context;

		for (Entry<String, StockInfo> entry : stock_list.entrySet()) {
			Log.i(LOG_TAG, "Updating current ticker for " + entry.getKey());
			client.get(
					"http://finance.yahoo.com/d/quotes.csv?s=" + entry.getKey()
							+ "&f=sbp2m2", new AsyncHttpResponseHandler() {
						@Override
						public void onSuccess(String response) {
							Log.i(LOG_TAG, "Got valid response: " + response);
							List<String> list = new ArrayList<String>(Arrays
									.asList(response.split(",")));

							String ticker = list.get(0).replace("\"", "");
							StockInfo toUpdate = stock_list.get(ticker);

							toUpdate.time = new SimpleDateFormat("H:mm")
									.format(Calendar.getInstance().getTime());

							toUpdate.price = Double.parseDouble(list.get(1));
							notify_canvas_updates_available(TICKER_TEXT_ID,
									keepContext);
						}
					});

			Log.i(LOG_TAG, "Getting day data for " + entry.getKey());
			client.get("http://chartapi.finance.yahoo.com/instrument/1.0/"
					+ entry.getKey() + "/chartdata;type=quote;range=1d/csv",
					new AsyncHttpResponseHandler() {
						@Override
						public void onSuccess(String response) {
							Log.i(LOG_TAG, "Got valid day data response");
							// Strip out all the data at the top, leaving just the tick data. The ?s thing
							// does it multi-line
							String ticker = response.replaceAll("(?s).*ticker:(\\w{4}).*", "$1");
							ticker = ticker.toUpperCase();
							Log.i(LOG_TAG, "Got day data back for " + ticker);
							
							response = response.replaceAll("(?s).*volume:[0-9\\,]*","");
							
							// We now have the data on a sequence of lines - need to dump all but the prices
							response = response.replaceAll("[0-9]*,","");
							Log.i(LOG_TAG, "Got response " + response);
							//response = response.replaceAll("^([0-9]\\.*),.*","$1");
						}
					});

		}

	}

}
