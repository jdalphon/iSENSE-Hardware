package edu.cs.uml.isense.datawalk;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.cs.uml.isense.R;
import edu.uml.cs.isense.comm.RestAPI;

public class DataWalk extends Activity implements SensorEventListener, LocationListener {

	private Button startStop;
	private TextView values;
	private Boolean running = false;
	private Vibrator vibrator;
	
	private SensorManager mSensorManager;
	private LocationManager mLocationManager;
	private PowerManager mPowerManager;
	private WakeLock runLock;
	
	private Location loc;
	private float accel[];
	private float orientation[];
	private Timer timeTimer;
	private float rawAccel[];
	private float rawMag[];
	
	public static String firstName = "";
    public static String lastInitial = "";
	    
    public static final int DIALOG_CANCELED    = 0;
    public static final int DIALOG_OK          = 1;
    
	private static final int MENU_ITEM_ABOUT   = 2;
	private static final int DIALOG_VIEW_DATA  = 3;
	private static final int DIALOG_SUMMARY    = 4;
	private static final int DIALOG_NEED_NAME  = 5;
	private static final int DIALOG_NO_GPS     = 6;
	private static final int DIALOG_FORCE_STOP = 7;
	private static final int RECORDING_STOPPED = 8;
	
	private static final int INTERVAL          = 10000;
    
	private int count          =  0;
	private int	elapsedMillis  =  0;
    private int	totalMillis    =  0;
    private int dataPointCount =  0;
    private int sessionId      = -1;
    
    private MediaPlayer mMediaPlayer;
        
    RestAPI rapi = null;
    
    String s_elapsedSeconds, s_elapsedMillis, s_elapsedMinutes;
    String nameOfSession = "";
    String partialSessionName = "";
    
    DecimalFormat toThou = new DecimalFormat("#,###,##0.000");
    
    int i = 0, len = 0, len2 = 0;
    
    ProgressDialog dia;
    double partialProg = 1.0;
    
    private boolean throughHandler   = false;
    
    static boolean inPausedState     = false;
    static boolean toastSuccess      = false;
    static boolean setupDone         = false;
    static boolean choiceViaMenu     = false;
    static boolean dontToastMeTwice  = false;
    static boolean exitAppViaBack    = false;
    static boolean backWasPressed    = false;
    static boolean useMenu           = true ;
    static boolean beginWrite        = true ;
        
    private Handler mHandler;
    
    public static String textToSession = "";
    public static String toSendOut = "";
    
    private static String loginName = "accelapp";
    private static String loginPass = "ecgrul3s";
    private static String experimentId = "387";
    private static String baseSessionUrl = "http://isensedev.cs.uml.edu/vis.php?sessions=";
	private static String sessionUrl = "";
    
    public static JSONArray dataSet;
    
    static int mheight = 1;
	static int mwidth = 1;
	
	public static Context mContext;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mContext = this;
        
        mPowerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        runLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        
        Display deviceDisplay = getWindowManager().getDefaultDisplay(); 
    	mwidth  = deviceDisplay.getWidth();
    	mheight = deviceDisplay.getHeight();
        
        // Display the End User Agreement
        AlertDialog.Builder adb = new SimpleEula(this).show();
        if(adb != null) {
        	Dialog dialog = adb.create();
        	
        	Display display = getWindowManager().getDefaultDisplay(); 
        	int mwidth = display.getWidth();
        	int mheight = display.getHeight();
        	
        	dialog.show();
        	
        	int apiLevel = getApiLevel();
        	if(apiLevel >= 11) {

        		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        	
        		lp.copyFrom(dialog.getWindow().getAttributes());
        		lp.width = mwidth;
        		lp.height = mheight;
        		lp.gravity = Gravity.CENTER_VERTICAL;
        		lp.dimAmount=0.7f;
	    	
        		dialog.getWindow().setAttributes(lp);
        		dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        		
        	}
        }
       
        rapi = RestAPI.getInstance((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE), getApplicationContext());
        
        boolean Login;
        if (rapi != null) {
           	while ((Login = rapi.login(loginName, loginPass)) == false) {
        		Log.d("Login", "Login in successful: " + Login);
        	}
        } else Toast.makeText(mContext, "OMG", Toast.LENGTH_SHORT);
        
        mHandler = new Handler();
        
        startStop = (Button) findViewById(R.id.startStop);
              
        values = (TextView) findViewById(R.id.values);
        
        
        /* This block useful for if onBackPressed - retains some things from previous session */
        if(running)
    		showDialog(DIALOG_FORCE_STOP);
    	   	rapi.login(loginName, loginPass); 
    	            	
        
        startStop.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
        startStop.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				
					vibrator.vibrate(300);
					mMediaPlayer.setLooping(false);  
					mMediaPlayer.start();
					
					if (running) {
						
						mSensorManager.unregisterListener(DataWalk.this);
						running = false;
						startStop.setText("Hold to Start");
						 
						timeTimer.cancel();
						
						startStop.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
						if(throughHandler)
							showDialog(RECORDING_STOPPED);
					
						running = false; 
						
						if (runLock.isHeld()) runLock.release();
					
					} else {
						
				        runLock.acquire();
						
						elapsedMillis = 0; totalMillis    = 0;
						len = 0; len2 = 0; dataPointCount = 0;
						i   = 0;
						
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							Toast.makeText( getBaseContext() , "Data recording interrupted! Time values may be inconsistent." , Toast.LENGTH_SHORT).show();
							e.printStackTrace();
						}

						useMenu = false;
						
						if (mSensorManager != null) {
							mSensorManager.registerListener(DataWalk.this, 
									mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),  
									SensorManager.SENSOR_DELAY_FASTEST);
							mSensorManager.registerListener(DataWalk.this, 
									mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 
									SensorManager.SENSOR_DELAY_FASTEST);
						}
						
						running = true;
						startStop.setText("Hold to Stop");
			    	
						timeTimer = new Timer();
						timeTimer.scheduleAtFixedRate(new TimerTask() {
							public void run() {
							
								dataPointCount++;
								count = 0;
								elapsedMillis += INTERVAL;
								totalMillis = elapsedMillis;
			
								if(i >= 360) {
								
									timeTimer.cancel();
									
									mHandler.post(new Runnable() {
										@Override
										public void run() {
											throughHandler = true;
											startStop.performLongClick();
										}
									});
								
								} else {
								
									i++; len++; len2++;	
									
									JSONArray dataJSON = new JSONArray();
								    JSONArray dataSetNew = new JSONArray();
								    
									try {
										
										/* Accel-x    */ dataJSON.put(toThou.format(accel[0]));
										/* Accel-y    */ dataJSON.put(toThou.format(accel[1]));
										/* Accel-z    */ dataJSON.put(toThou.format(accel[2]));
										/* Accel-Total*/ dataJSON.put(toThou.format(accel[3]));
										/* Latitude   */ dataJSON.put(loc.getLatitude());
										/* Longitude  */ dataJSON.put(loc.getLongitude());
										/* Heading    */ dataJSON.put(toThou.format(orientation[0]));
										/* Magnetic-x */ dataJSON.put(rawMag[0]);
										/* Magnetic-y */ dataJSON.put(rawMag[1]);
										/* Magnetic-z */ dataJSON.put(rawMag[2]);
										/* Time       */ dataJSON.put(elapsedMillis); 
										
										dataSetNew.put(dataJSON);
										dataSet = dataSetNew;
										Log.d("DataSet", "DataSet" + dataSet.toString());
										
									} catch (JSONException e) {
										e.printStackTrace();
									}
									
									mHandler.post(new Runnable() {
										@Override
										public void run() {
											new Task().execute();
										}
									});
								}
							
							}
						}, 0, INTERVAL);
						startStop.getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
					
					}	return running;
				
				
        }
        	
        });
        
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                      
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        	mLocationManager.requestLocationUpdates(mLocationManager.getBestProvider(c, true), 0, 0, DataWalk.this);
        else {
        	showDialog(DIALOG_NO_GPS);
        }
        
        accel       = new float[4];
        orientation = new float[3];
        rawAccel    = new float[3];
        rawMag      = new float[3];
        loc         = new Location(mLocationManager.getBestProvider(c, true));
        
        mMediaPlayer = MediaPlayer.create(this, R.raw.beep); 
        
        if(firstName.length() == 0 || lastInitial.length() == 0)
        	showDialog(DIALOG_NEED_NAME);
        
    } 
 
	@Override
    public void onPause() {
    	super.onPause();
    	mLocationManager.removeUpdates(DataWalk.this);
    	mSensorManager.unregisterListener(DataWalk.this);
    	if (timeTimer != null) timeTimer.cancel();
    	inPausedState = true;
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	mLocationManager.removeUpdates(DataWalk.this);
    	mSensorManager.unregisterListener(DataWalk.this);
    	if (timeTimer != null) timeTimer.cancel();
    	inPausedState = true;
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	inPausedState = false;
    	
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	inPausedState = false;
    	if(running)
    		showDialog(DIALOG_FORCE_STOP);
    		rapi.login(loginName, loginPass);
    }
    
    @Override
    public void onBackPressed() {
    	if(!dontToastMeTwice) {
    		if(running) 
    			Toast.makeText(this, "Cannot exit via BACK while recording data; use HOME instead.",
    					Toast.LENGTH_LONG).show();
    		else
    			Toast.makeText(this, "Press back again to exit.", Toast.LENGTH_SHORT).show();
    		new NoToastTwiceTask().execute();
    	} else if(exitAppViaBack && !running) {
    		
    		super.onBackPressed();	
    	}
    }
    
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {	
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		DecimalFormat oneDigit = new DecimalFormat("#,##0.0");

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			rawAccel = event.values.clone();
			accel[0] = event.values[0];
			accel[1] = event.values[1];
			accel[2] = event.values[2];
			
			String xPrepend = accel[0] > 0 ? "+" : "";
			String yPrepend = accel[1] > 0 ? "+" : "";
			String zPrepend = accel[2] > 0 ? "+" : "";

			if (count == 0) {
				values.setText("X: " + xPrepend + oneDigit.format(accel[0]) + ", Y: " + 
						yPrepend + oneDigit.format(accel[1]) + ", Z: " + 
						zPrepend + oneDigit.format(accel[2]));
			}
			
			accel[3] = (float) Math.sqrt(Math.pow(accel[0], 2) + Math.pow(accel[1], 2) + Math.pow(accel[2], 2));

		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			rawMag = event.values.clone();
			
			float rotation[] = new float[9];
			
			if (SensorManager.getRotationMatrix(rotation, null, rawAccel, rawMag)) {
				orientation = new float[3];
				SensorManager.getOrientation(rotation, orientation);
			}
			
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		loc = location;
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
    
	
	protected Dialog onCreateDialog(final int id) {
	    
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	Dialog dialog;
    	
    	WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
    	
	    switch(id) {
	    case DIALOG_NO_GPS:
	    	
	    	builder.setTitle("No GPS Provider Found")
	    	.setMessage("Enabling GPS satellites is recommended for this application.  Would you like to enable GPS?")
	    	.setCancelable(false)
	    	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialoginterface, final int id) {
	            	   dialoginterface.cancel();
	            	   startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
	               }
	           })
	           .setNegativeButton("No", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialoginterface, final int id) {
	                    dialoginterface.cancel();
	               }
	           });

	    	dialog = builder.create();
	    
	    	break;
	    	
	    case DIALOG_FORCE_STOP:
	    	
	    	builder.setTitle("Data Recording Halted")
	    	.setMessage("You exited the app while data was still being recorded.  Data has stopped recording.")
	    	.setCancelable(false)
	    	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialoginterface, final int id) {
	            	   dialoginterface.dismiss();
	            	   startStop.performLongClick();
	               }
	         });
	           
	    	dialog = builder.create();
	    	
	    	if (runLock.isHeld()) runLock.release();
	    
	    	break;
	    	
	    case RECORDING_STOPPED:
	    	
	    throughHandler = false;
	    	
	    builder.setTitle("Time Up")
	    .setMessage("You have been recording data for more than 1 hour.  For the sake of battery usage" +
	    		"responsibility, we have capped your maximum recording time at 1 hour and have stopped" +
	    		"recording for you.  Press OK to continue.")
	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialoginterface,int i) {
	    		dialoginterface.dismiss();
	    	}
	    })
     	  .setCancelable(false);
	    	
	    dialog = builder.create();
	    
        if (runLock.isHeld()) runLock.release();
	    
	    break;
	    	
	    case DIALOG_NEED_NAME:
	    	LoginActivity la = new LoginActivity(mContext);
	        dialog = la.getDialog(new Handler() {
			      public void handleMessage(Message msg) { 
			    	  switch (msg.what) {
			    	  	case LoginActivity.NAME_SUCCESSFULL:
			    	  	  break;
			    	  	case LoginActivity.NAME_CANCELED:
			    		  break;
			    	  	case LoginActivity.NAME_FAILED:
				    	  showDialog(DIALOG_NEED_NAME);
			    		  break;
			    	  }
			      }
        		});
                
	        break;
	    	
	    case DIALOG_VIEW_DATA:
	    	
	    	builder.setTitle("Web Browser")
	    	.setMessage("Would you like to view your data on the iSENSE website?")
	    	.setCancelable(false)
	    	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialoginterface, final int id) {
	            	   dialoginterface.dismiss();
	            	   Intent i = new Intent(Intent.ACTION_VIEW);
	            	   i.setData(Uri.parse(sessionUrl));
	            	   startActivity(i);
	               }
	    	})
	    	.setNegativeButton("No", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialoginterface, final int id) {
	            	   dialoginterface.dismiss();
	               }
	    	})
	        .setCancelable(true);
	           
	    	dialog = builder.create();
	    
	    	break;
	    	
	    case MENU_ITEM_ABOUT:
	    	
	    	builder.setTitle("About")
	    	.setMessage("This app has been solely designed for demonstration at USASEF.  The intended use of this app is for " +
	    				"users to be able to leave their phone in their pocket while the application runs and uploads data." +
	    				
	    				/**Fix this*/
	    				"When ready, the user shall press the \"Hold to Start\" button to begin recording Y and Z accelerometer " +
	    				"points as the vehicle slides/drives down the incline.  Data recording will run for 10 seconds, and the user " +
	    				"will then be prompted to upload their data or throw it away.  Should the user choose to upload the " +
	    				"data, he or she may then visualize it live on the iSENSE website (isenseproject.org).  The purpose of " +
	    				"prompting the user for his or her first name/last initial is solely for the identification of his or " +
	    				"her own sessions on the iSENSE website.  The user is not allowed to exit this app while recording data " +
	    				"via the back button.  However, if the user presses the home button, this app will pause.  Upon this app " +
	    				"resuming, the user will notice a dialog box that informs him or her of the action they took to pause the app, " +
	    				"and the data recording will halt/be thrown away.")
	    	.setCancelable(false)
	    	.setNegativeButton("Back", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialoginterface, final int id) {
	            	   dialoginterface.dismiss();
	               }
	    	})
	        .setCancelable(true);
	           
	    	dialog = builder.create();
	    
	    	break;
	    
	    default:
	    	dialog = null;
	    	break;
	    }
	    
	
	    int apiLevel = getApiLevel();
	    if(apiLevel >= 11) {
	    	dialog.show(); /* works but doesn't center it */
		    	
		   	lp.copyFrom(dialog.getWindow().getAttributes());
		   	lp.width = mwidth;
		   	lp.height = WindowManager.LayoutParams.MATCH_PARENT;
		   	lp.gravity = Gravity.CENTER_VERTICAL;
		   	lp.dimAmount=0.7f;
		   	
		   	dialog.getWindow().setAttributes(lp);
		    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		    	
		    dialog.setOnDismissListener(new OnDismissListener() {
	           	@Override
	           	public void onDismiss(DialogInterface dialog) {
	           		removeDialog(id);
	           	}
	        });
		    	
		   	return null;
		    	
	    } else {
	    		
	    	dialog.setOnDismissListener(new OnDismissListener() {
	            @Override
	            public void onDismiss(DialogInterface dialog) {
	            	removeDialog(id);
	            }
	        });
	    		
	    	return dialog;
	    } 	    
	    
	}

    
    
    
    static int getApiLevel() {
    	return Integer.parseInt(android.os.Build.VERSION.SDK);
    }
    
	
	private Runnable uploader = new Runnable() {
		
		@Override
		public void run() {
		
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy, HH:mm:ss");
			Date dt = new Date();
			String dateString = sdf.format(dt);
		    
			nameOfSession = firstName + " " +  lastInitial + ". - " + dateString;
		
			if (sessionId == -1) {
				if(nameOfSession.equals("")) {
					sessionId = rapi.createSession(experimentId, 
								"*Session Name Not Provided*", 
								"Automated Submission Through Android App", 
								"N/A", "N/A", "United States");
					boolean hasPut = rapi.putSessionData( sessionId, experimentId, dataSet);
					Log.d("Upload", "Upload Status: "+hasPut);
				} else {
					sessionId = rapi.createSession(experimentId, 
							nameOfSession, 
							"Automated Submission Through Android App", 
							"N/A", "N/A", "United States");
					boolean hasPut = rapi.putSessionData( sessionId, experimentId, dataSet);
					Log.d("Upload", "Upload Status: "+hasPut);
				}
			} else {
				boolean appendSuccess = rapi.updateSessionData(sessionId, experimentId, dataSet);
				Log.d("Upload", "Upload Append: " + appendSuccess);
			}
		
		}
		
	};

	private class Task extends AsyncTask <Void, Integer, Void> {
	    
	    @Override protected void onPreExecute() {
	    	
	        dia = new ProgressDialog(DataWalk.this);
	        dia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	        dia.setMessage("Please wait while your data is uploaded to iSENSE...");
	        dia.setCancelable(false);
	        dia.show();
	        
	    }

	    @Override protected Void doInBackground(Void... voids) {

	        uploader.run();
	        publishProgress(100);
	        return null;
	        
	    }

	    @Override  protected void onPostExecute(Void voids) {
	        
	    	dia.setMessage("Done");
	        dia.cancel();
	        
	        len = 0; len2 = 0;
	        
	    }
	}	
	
	private class NoToastTwiceTask extends AsyncTask <Void, Integer, Void> {
	    @Override protected void onPreExecute() {
	    	dontToastMeTwice = true;
	    	exitAppViaBack   = true;
	    }
		@Override protected Void doInBackground(Void... voids) {
	    	try {
	    		Thread.sleep(1500);
	    		exitAppViaBack = false;
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				exitAppViaBack = false;
				e.printStackTrace();
			}
	        return null;
		}
	    @Override  protected void onPostExecute(Void voids) {
	    	dontToastMeTwice = false;
	    }
	}
}