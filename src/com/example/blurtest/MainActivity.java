package com.example.blurtest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

public class MainActivity extends Activity implements
		GestureDetector.OnGestureListener {
	GestureDetector mGestreDetector = null;
	TextView mLogMsg = null;
	Bitmap mBitmap = null;
	float mFac = 4;
	int mKermalR = 20;
	View mDecorView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		 mDecorView = getWindow().getDecorView();
		setContentView(R.layout.activity_main);
	       
		mGestreDetector = new GestureDetector(this, this);
		mLogMsg = (TextView) findViewById(R.id.log_msg);

		mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.window_bg);

		findViewById(R.id.main_view).setOnTouchListener(
				new View.OnTouchListener() {

					@Override
					public boolean onTouch(View arg0, MotionEvent arg1) {
						return mGestreDetector.onTouchEvent(arg1);
					}
				});
		getWindow().getDecorView().getViewTreeObserver()
				.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					boolean only_once = true;

					@Override
					public boolean onPreDraw() {

						if (only_once == true) {
							mBitmap = utils
									.getSnapshotBitmap(findViewById(R.id.main_view));
							mBitmap = utils.shrink(mBitmap, 1 / mFac);
							mBitmap = utils.fastblur(mBitmap, mKermalR);
							only_once = false;
						}

						return true;
					}
				});
		
		hideSystemUI();

	}

	// This snippet hides the system bars.
	private void hideSystemUI() {
	    // Set the IMMERSIVE flag.
	    // Set the content to appear under the system bars so that the content
	    // doesn't resize when the system bars hide and show.
	    mDecorView.setSystemUiVisibility(
	            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
	            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
	            | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	// This snippet shows the system bars. It does this by removing all the flags
	// except for the ones that make the content appear under the system bars.
	private void showSystemUI() {
	    mDecorView.setSystemUiVisibility(
	            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}
	
	@Override
	public boolean onDown(MotionEvent paramMotionEvent) {
		hideSystemUI();
		return true;
	}

	@Override
	public void onShowPress(MotionEvent paramMotionEvent) {
		showSystemUI();
	}

	@Override
	public boolean onSingleTapUp(MotionEvent paramMotionEvent) {
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent paramMotionEvent1,
			MotionEvent paramMotionEvent2, float paramFloat1, float paramFloat2) {
		long startMs = System.currentTimeMillis();
		View view = findViewById(R.id.blur_test);

		view.setX(view.getX() - paramFloat1);
		view.setY(view.getY() - paramFloat2);

		Rect rc = new Rect((int) ((view.getX()) / mFac),
				(int) ((view.getY()) / mFac),
				(int) ((view.getX() + view.getWidth()) / mFac),
				(int) ((view.getY() + view.getHeight()) / mFac));

		Bitmap bk = utils.getSubBitmap(mBitmap, rc);

		view.setBackgroundDrawable(new BitmapDrawable(getResources(), bk));
		mLogMsg.setText(System.currentTimeMillis() - startMs + "ms");
		return false;
	}

	@Override
	public void onLongPress(MotionEvent paramMotionEvent) {
	}

	@Override
	public boolean onFling(MotionEvent paramMotionEvent1,
			MotionEvent paramMotionEvent2, float paramFloat1, float paramFloat2) {
		return false;
	}

}
