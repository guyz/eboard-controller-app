package edu.mit.media.eboardcontroller;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import edu.mit.media.eboardcontroller.util.SystemUiHider;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.view.GestureDetector;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class ThrottleActivity extends Activity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
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
	private GestureDetectorCompat mDetector;
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
	
	// BT related
	private static final String TAG = "bluetooth1";
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;
    
	// SPP UUID service 
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  
  	// MAC-address of Bluetooth module (you must edit this line)
  	private static String address = "20:14:10:28:03:91";
  
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
	    
	    mDetector.onTouchEvent(event);
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
    		
    		int cIntensity = 255 - (int) (Math.abs(this.speed) * 255);
    		int c = (255 << 8*3) + (cIntensity << 8*2) + (255 << 8) + cIntensity;
    		mainlayoutcontent.setBackgroundColor(c);
    		
    		// BT
    		Log.d(TAG, "Sending spd=" + this.speed);
    		sendData("spd=" + this.speed);
    	}
		
	}
	
	private void sendStop() {
		this.speed = 0;
		// Set text
		TextView speedcontentView = (TextView) findViewById(R.id.speed_content);
		speedcontentView.setText( String.format("%.1f", this.speed * 100.0 ) );
		
		// Set background
		View mainlayoutcontent = findViewById(R.id.mainlayout_content);
		
		int cIntensity = 255 - (int) (Math.abs(this.speed) * 255);
		int c = (255 << 8*3) + (cIntensity << 8*2) + (255 << 8) + cIntensity;
		mainlayoutcontent.setBackgroundColor(c);
		
		// BT
		Log.d(TAG, "Sending spd=" + this.speed);
		sendData("spd=" + this.speed);
	}
	
	private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
	      try {
	    	  Method m=device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
	    	  return (BluetoothSocket) m.invoke(device, 1);
	      } catch (Exception e) {
        	  Log.e(TAG, "Could not create Insecure RFComm Connection",e);
          }
	      
	      return null;
	  }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_throttle);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View speedcontentView = findViewById(R.id.speed_content);
		final View screencontentView = findViewById(R.id.screen_content);
		final View mainlayoutcontent = findViewById(R.id.mainlayout_content);
		
		mDetector = new GestureDetectorCompat(this,(OnGestureListener) this);
        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);
        
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		this.screenHeight = (double) size.y;
		
		// BT
		btAdapter = BluetoothAdapter.getDefaultAdapter();
	    checkBTState();
		
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
	}
	
	  @Override
	  public void onResume() {
	    super.onResume();
	  
	    Log.d(TAG, "...onResume - try connect...");
	    
	    // Set up a pointer to the remote node using it's address.
	    BluetoothDevice device = btAdapter.getRemoteDevice(address);
	    
	    // Two things are needed to make a connection:
	    //   A MAC address, which we got above.
	    //   A Service ID or UUID.  In this case we are using the
	    //     UUID for SPP.
	    
	    try {
	        btSocket = createBluetoothSocket(device);
	    } catch (IOException e1) {
	        errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
	    }
	    
	    if (btSocket == null) {
	    	errorExit("Fatal Error", "Can't create BT socket");
	    	return;
	    }
	        
	    // Discovery is resource intensive.  Make sure it isn't going on
	    // when you attempt to connect and pass your message.
	    btAdapter.cancelDiscovery();
	    
	    // Establish the connection.  This will block until it connects.
	    Log.d(TAG, "...Connecting...");
	    try {
	      btSocket.connect();
	      Log.d(TAG, "...Connection ok...");
	    } catch (IOException e) {
	      try {
	        btSocket.close();
	      } catch (IOException e2) {
	        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
	      }
	    }
	      
	    // Create a data stream so we can talk to server.
	    Log.d(TAG, "...Create Socket...");
	  
	    try {
	      outStream = btSocket.getOutputStream();
	      
	      // Ask to reset the connection
	      sendData("rst");
	    } catch (IOException e) {
	      errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
	    }
	  }
	  
	  @Override
	  public void onPause() {
	    super.onPause();
	  
	    Log.d(TAG, "...In onPause()...");
	  
	    if (outStream != null) {
	      try {
	        outStream.flush();
	      } catch (IOException e) {
	        errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
	      }
	    }
	  
	    try     {
	      btSocket.close();
	    } catch (IOException e2) {
	      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
	    }
	  }
	    
	  private void checkBTState() {
	    // Check for Bluetooth support and then check to make sure it is turned on
	    // Emulator doesn't support Bluetooth and will return null
	    if(btAdapter==null) { 
	      errorExit("Fatal Error", "Bluetooth not support");
	    } else {
	      if (btAdapter.isEnabled()) {
	        Log.d(TAG, "...Bluetooth ON...");
	      } else {
	        //Prompt user to turn on Bluetooth
	        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableBtIntent, 1);
	      }
	    }
	  }
	  
	  private void errorExit(String title, String message){
	    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
	  }
	  
	  private void sendData(String message) {
	    byte[] msgBuffer = message.getBytes();
	  
	    Log.d(TAG, "...Send data: " + message + "...");
	  
	    try {
	      outStream.write(msgBuffer);
	    } catch (IOException e) {
	      String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
	      if (address.equals("00:00:00:00:00:00")) 
	        msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
	        msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
	        
	        errorExit("Fatal Error", msg);       
	    }
	  }
	
	@Override
	public boolean onDown(MotionEvent event) { 
	    Log.d(TAG,"onDown: " + event.toString()); 
	    return true;
	}
	
	@Override
	public boolean onFling(MotionEvent event1, MotionEvent event2, 
	        float velocityX, float velocityY) {
	    Log.d(TAG, "onFling: " + event1.toString()+event2.toString());
	    return true;
	}
	
	@Override
	public void onLongPress(MotionEvent event) {
	    Log.d(TAG, "onLongPress: " + event.toString()); 
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
	        float distanceY) {
//	    Log.d(TAG, "onScroll: " + e1.toString()+e2.toString());
	    return true;
	}
	
	@Override
	public void onShowPress(MotionEvent event) {
	    Log.d(TAG, "onShowPress: " + event.toString());
	}
	
	@Override
	public boolean onSingleTapUp(MotionEvent event) {
//	    Log.d(TAG, "onSingleTapUp: " + event.toString());
	    return true;
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent event) {
//	    Log.d(TAG, "onDoubleTap: " + event.toString());
		sendStop();
	    return true;
	}
	
	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
//	    Log.d(TAG, "onDoubleTapEvent: " + event.toString());
	    return true;
	}
	
	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
//	    Log.d(TAG, "onSingleTapConfirmed: " + event.toString());
	    return true;
	}
	    
}
