package edu.mit.media.eboardcontroller;

import edu.mit.media.eboardcontroller.util.SystemUiHider;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class ThrottleActivity extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	private static final double EPS = 0.05;
	private static final int MAX_COLOR = 163;
	
	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	private boolean isCruise = false;
	private double speed = 0;
	private double direction = 1; // either 1 or -1
	private double startPos = 0;
	private double screenHeight = 0;
	
	/* Event states:
	 * 		0 = no touch. If speed != 0, decelerate to 0
	 * 		1 = first touch. Still need to determine direction
	 * 		2 = forward direction
	 * 		3 = backward direction
	 */
	private int state = 0; // Event state

	@Override
	public boolean onTouchEvent(MotionEvent event) {
//	    int x = (int)event.getX();
	    int y = (int)event.getY();
	    switch (event.getAction()) {
	        case MotionEvent.ACTION_DOWN:
	        	this.state = 1;
	        	this.startPos = (double) y;
	        case MotionEvent.ACTION_MOVE:
	        	updateMovement(y);
	        case MotionEvent.ACTION_UP:
	        	this.state = 0;
	        	
	        	// TODO: start decelerating ...
	    }
	    
	    return false;
	}
	
	private void updateMovement(int y) {
		double dy = ( this.startPos - y ) / this.screenHeight;
    	if (Math.abs(dy) >= this.EPS) {
    		if (Math.abs(this.speed + dy) > 1) {
    			if (this.speed + dy < 0) {
    				this.speed = -1;
    			} else {
    				this.speed = 1;
    			}
    		} else {
    			this.speed += dy;
    		}
    		this.startPos = (double) y;
    		
    		// Set text
    		TextView speedcontentView = (TextView) findViewById(R.id.speed_content);
    		speedcontentView.setText( String.format("%.1f", this.speed * 100.0 ) );
    		
    		// Set background
    		View mainlayoutcontent = findViewById(R.id.mainlayout_content);
    		
//    		int dc = 255 - this.MAX_COLOR;
//    		int cIntensity = 255 - (int) (this.speed * dc);
    		int cIntensity = 255 - (int) (Math.abs(this.speed) * 255);
    		int c = (255 << 8*3) + (cIntensity << 8*2) + (255 << 8) + cIntensity;
    		mainlayoutcontent.setBackgroundColor(c);
    		
//    		mainlayoutcontent.setAlpha( (int) ( this.speed * 100 ) );
    		
//    		Drawable background = screencontentView.getBackground();
//    		background.setAlpha( (int) ( this.speed * 100 ) );
    		
    		// TODO: BT
    	}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_throttle);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View speedcontentView = findViewById(R.id.speed_content);
		final View screencontentView = findViewById(R.id.screen_content);
		final View mainlayoutcontent = findViewById(R.id.mainlayout_content);
		
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		this.screenHeight = (double) size.y;
		
		//animate from your current color to red
//		ValueAnimator anim = ValueAnimator.ofInt(Color.parseColor("#00FF00"), Color.parseColor("#FFFFFF"));
		ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
		
		anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
		    @Override
		    public void onAnimationUpdate(ValueAnimator animation) {
//		    	mainlayoutcontent.setBackgroundColor((Integer) animation.getAnimatedValue());
		    	float v = (Float) animation.getAnimatedValue();
	    		int cIntensity = (int) (v * 255);
	    		int c = (255 << 8*3) + (cIntensity << 8*2) + (255 << 8) + cIntensity;
	    		mainlayoutcontent.setBackgroundColor(c);
	    		
	    		TextView speedcontentView = (TextView) findViewById(R.id.speed_content);
	    		speedcontentView.setText( String.format("%.1f", (1.0 - v) * 100.0 ) );
		    }
		});
//		anim.setEvaluator(new ArgbEvaluator());
		anim.setDuration(2000);
		anim.start();
		
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
//		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
//				HIDER_FLAGS);
//		mSystemUiHider.setup();
//		mSystemUiHider
//				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
//					// Cached values.
//					int mControlsHeight;
//					int mShortAnimTime;
//
//					@Override
//					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
//					public void onVisibilityChange(boolean visible) {
//						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
//							// If the ViewPropertyAnimator API is available
//							// (Honeycomb MR2 and later), use it to animate the
//							// in-layout UI controls at the bottom of the
//							// screen.
//							if (mControlsHeight == 0) {
//								mControlsHeight = controlsView.getHeight();
//							}
//							if (mShortAnimTime == 0) {
//								mShortAnimTime = getResources().getInteger(
//										android.R.integer.config_shortAnimTime);
//							}
//							controlsView
//									.animate()
//									.translationY(visible ? 0 : mControlsHeight)
//									.setDuration(mShortAnimTime);
//						} else {
//							// If the ViewPropertyAnimator APIs aren't
//							// available, simply show or hide the in-layout UI
//							// controls.
//							controlsView.setVisibility(visible ? View.VISIBLE
//									: View.GONE);
//						}
//
//						if (visible && AUTO_HIDE) {
//							// Schedule a hide().
//							delayedHide(AUTO_HIDE_DELAY_MILLIS);
//						}
//					}
//				});

		// Set up the user interaction to manually show or hide the system UI.
//		contentView.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View view) {
//				if (TOGGLE_ON_CLICK) {
//					mSystemUiHider.toggle();
//				} else {
//					mSystemUiHider.show();
//				}
//			}
//		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
//		findViewById(R.id.dummy_button).setOnTouchListener(
//				mDelayHideTouchListener);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
//		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
//	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
//		@Override
//		public boolean onTouch(View view, MotionEvent motionEvent) {
//			if (AUTO_HIDE) {
//				delayedHide(AUTO_HIDE_DELAY_MILLIS);
//			}
//			return false;
//		}
//	};

//	Handler mHideHandler = new Handler();
//	Runnable mHideRunnable = new Runnable() {
//		@Override
//		public void run() {
//			mSystemUiHider.hide();
//		}
//	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
//	private void delayedHide(int delayMillis) {
//		mHideHandler.removeCallbacks(mHideRunnable);
//		mHideHandler.postDelayed(mHideRunnable, delayMillis);
//	}
}
