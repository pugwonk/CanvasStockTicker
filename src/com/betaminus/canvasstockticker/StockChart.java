package com.betaminus.canvasstockticker;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class StockChart {
	private GraphicalView mChart;

	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

	public XYSeries mCurrentSeries;

	private XYSeriesRenderer mCurrentRenderer;

	private void initChart() {
		mCurrentSeries = new XYSeries("Sample Data");
		mDataset.addSeries(mCurrentSeries);
		mCurrentRenderer = new XYSeriesRenderer();
		mCurrentRenderer.setShowLegendItem(false);
		mCurrentRenderer.setLineWidth(4f);
		mCurrentRenderer.setColor(Color.WHITE);
		mRenderer.addSeriesRenderer(mCurrentRenderer);
		mRenderer.setShowAxes(false);
		mRenderer.setXLabels(0);
		mRenderer.setYLabels(0);
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setBackgroundColor(Color.BLACK);
		//mRenderer.setMarginsColor(Color.BLACK);
	}

	private void addSampleData() {
		mCurrentSeries.add(1, 2);
		mCurrentSeries.add(2, 3);
		mCurrentSeries.add(3, 2);
		mCurrentSeries.add(4, 5);
		mCurrentSeries.add(5, 4);
	}

	public StockChart(Context context) {
		initChart();
		//addSampleData();
		mChart = ChartFactory.getLineChartView(context, mDataset, mRenderer);
	}
	
	public Bitmap getBitmap() {
		return bitmapFromChartView(mChart, 200, 200);
	}

	private Bitmap loadBitmapFromView(View v) {
		v.setDrawingCacheEnabled(true);
		if (v.getMeasuredHeight() <= 0) {
			v.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(),
					v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(b);
			v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
			v.draw(c);
			return b;
		} else {
			return null;
		}
	}

	private Bitmap bitmapFromChartView(View v, int width, int height) {
		Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		v.layout(0, 0, width, height);
		v.draw(c);
		return b;
	}

	private Bitmap loadBitmapFromView2(View v) {
		v.setDrawingCacheEnabled(true);
		v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

		v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

		v.buildDrawingCache(true);
		Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
		v.setDrawingCacheEnabled(false); // clear drawing cache
		return b;
	}

	public Bitmap getChartBitmap() {
		return loadBitmapFromView(mChart);
	}
}
