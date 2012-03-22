/***************************************************************************************************/
/***************************************************************************************************/
/**                                                                                               **/
/**      IIIIIIIIIIIII               iSENSE Car Ramp Physics App                 SSSSSSSSS        **/
/**           III                                                               SSS               **/
/**           III                    By: Michael Stowell                       SSS                **/
/**           III                    Some Code From: iSENSE Amusement Park      SSS               **/
/**           III                                    App (John Fertita)          SSSSSSSSS        **/
/**           III                    Faculty Advisor:  Fred Martin                      SSS       **/
/**           III                    Group:            ECG,                              SSS      **/
/**           III                                      iSENSE                           SSS       **/
/**      IIIIIIIIIIIII               Property:         UMass Lowell              SSSSSSSSS        **/
/**                                                                                               **/
/***************************************************************************************************/
/***************************************************************************************************/

package edu.uml.cs.isense.carphysics;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;

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
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import edu.uml.cs.isense.comm.RestAPI;

public class CarRampPhysics extends Activity implements SensorEventListener, LocationListener {
    
	private static String experimentNumber = "386";        // HARD CODED
	private static String userName         = "accelapp";   // HARD CODED
	private static String password         = "ecgrul3s";   // HARD CODED
	private static String baseSessionUrl = "http://isensedev.cs.uml.edu/vis.php?sessions=";
	private static String sessionUrl = "";
	
	private Button startStop;
	private TextView values;
	private Boolean running = false;
	//private Vibrator vibrator;
	
	private SensorManager mSensorManager;
	//private LocationManager mLocationManager;
	
	//private Location loc;
	private float accel[];
	private float orientation[];
	private Timer timeTimer;
	private float rawAccel[];
	private float rawMag[];
	
	private static final int INTERVAL = 50;
	private static final int DIALOG_CHOICE     = 1;
	private static final int DIALOG_FORCE_STOP = 2;
	private static final int DIALOG_NEED_NAME  = 3;
	private static final int DIALOG_VIEW_DATA  = 4;
	private static final int MENU_ITEM_ABOUT   = 5;
	private static final int MENU_ITEM_EULA    = 6;
	//private static final int EXPERIMENT_CODE = ;
	
    static final public int DIALOG_CANCELED = 0;
    static final public int DIALOG_OK = 1;
    static final public int DIALOG_PICTURE = 2;
    
    static final public int CAMERA_PIC_REQUESTED = 1;
    static final public int CAMERA_VID_REQUESTED = 2;
    	
    private int count = 0;
    private int countdown = 10;
    
    static String firstName = "";
    static String lastInitial = "";
    
    private boolean timeHasElapsed = false;
    private boolean usedHomeButton = false;
    
    private MediaPlayer mMediaPlayer;
 
    private int    elapsedMinutes =  0 ;
    private int    elapsedSeconds =  0 ;
    private int    elapsedMillis  =  0 ;
    private int    totalMillis    =  0 ;
    
    private int dataPointCount = 0;
    
    private String dateString;
    RestAPI rapi ;
    
    String s_elapsedSeconds, s_elapsedMillis, s_elapsedMinutes;
    DecimalFormat toThou = new DecimalFormat("#,###,##0.000");
    
    int i = 0;  int len = 0;  int len2 = 0;
    
    ProgressDialog dia;
    double partialProg = 1.0;
    
    private EditText sessionName; 
    String nameOfSession = "";
    String partialSessionName = "";
    
    static int     mediaCount        = 0;
    static boolean inPausedState     = false;
    static boolean toastSuccess      = false;
    static boolean useMenu           = true ;
    static boolean setupDone         = false;
    static boolean choiceViaMenu     = false;
    static boolean dontToastMeTwice  = false;
    static boolean exitAppViaBack    = false;
    static boolean backWasPressed    = false;
    static boolean nameSuccess       = false;
    
    private Handler mHandler;
    
    public static String textToSession = "";
    public static String toSendOut = "";
    public static String experimentId = "";
    public static JSONArray dataSet;
    
    static int mheight = 1;
	static int mwidth = 1;
	
	public static Context mContext;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mContext = this;
        
        Display deviceDisplay = getWindowManager().getDefaultDisplay(); 
    	mwidth  = deviceDisplay.getWidth();
    	mheight = deviceDisplay.getHeight();
        
        rapi = RestAPI.getInstance((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE), getApplicationContext());
        
        rapi.login(userName, password);
        
        mHandler = new Handler();
        
        startStop = (Button) findViewById(R.id.startStop);
              
        values = (TextView) findViewById(R.id.values);
        
        /* This block useful for if onBackPressed - retains some things from previous session */
        if(running)
    		showDialog(DIALOG_FORCE_STOP);
        
        startStop.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
        startStop.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
					
				//vibrator.vibrate(300);
				mMediaPlayer.setLooping(false);  
				mMediaPlayer.start();
					
				if (running) {
					
					if(timeHasElapsed) {
						setupDone = false;
						timeHasElapsed = false;
						useMenu = true;
						countdown = 10;
						
						mSensorManager.unregisterListener(CarRampPhysics.this);
						running = false;
						startStop.setText("Hold to Start");
							 
						timeTimer.cancel();
						count++;
						startStop.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
						choiceViaMenu = false;
						showDialog(DIALOG_CHOICE);
					} else if (usedHomeButton) {
						setupDone = false;
						timeHasElapsed = false;
						useMenu = true;
						countdown = 10;
						
						mSensorManager.unregisterListener(CarRampPhysics.this);
						running = false;
						startStop.setText("Hold to Start");
							 
						timeTimer.cancel();
						count++;
						startStop.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
						choiceViaMenu = false;
					}
					
					startStop.setEnabled(true);
						
				} else {
						
					startStop.setEnabled(false);
					dataSet = new JSONArray();
					elapsedMillis = 0; totalMillis    = 0;
					len = 0; len2 = 0; dataPointCount = 0;
					i   = 0;
					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Toast.makeText(getBaseContext() , "Data recording has offset 100 milliseconds due to an error." , Toast.LENGTH_SHORT).show();
						e.printStackTrace();
					}

					useMenu = false;
						
					if (mSensorManager != null) {
						mSensorManager.registerListener(CarRampPhysics.this, 
								mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),  
								SensorManager.SENSOR_DELAY_FASTEST);
						mSensorManager.registerListener(CarRampPhysics.this, 
								mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 
								SensorManager.SENSOR_DELAY_FASTEST);
					}
						
					running = true;
					startStop.setText("" + countdown);
			    	
					timeTimer = new Timer();
					timeTimer.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							
							dataPointCount++;
							count = (count + 1) % 1;
							elapsedMillis += INTERVAL;
							totalMillis = elapsedMillis;
			
							if(i >= 200) {
								
								timeTimer.cancel();
								timeHasElapsed = true;
									
								mHandler.post(new Runnable() {
									@Override
									public void run() {
										startStop.performLongClick();
									}
								});
								
							} else {
								
								i++; len++; len2++;	
								
								if (i % 20 == 0) {
									mHandler.post(new Runnable() {
										@Override
										public void run() {
											startStop.setText("" + countdown);
										}
									});
									countdown--;
								}
									
									
								JSONArray dataJSON = new JSONArray();
										
								/* Accel-x    */ //dataJSON.put(toThou.format(accel[0]));
								/* Accel-y    */ dataJSON.put(toThou.format(accel[1]));
								/* Accel-z    */ dataJSON.put(toThou.format(accel[2]));
								/* Accel-Total*/ //dataJSON.put(toThou.format(accel[3]));
								/* Latitude   */ //dataJSON.put(loc.getLatitude());
								/* Longitude  */ //dataJSON.put(loc.getLongitude());
								/* Heading    */ //dataJSON.put(toThou.format(orientation[0]));
								/* Magnetic-x */ //dataJSON.put(rawMag[0]);
								/* Magnetic-y */ //dataJSON.put(rawMag[1]);
								/* Magnetic-z */ //dataJSON.put(rawMag[2]);
								/* Time       */ dataJSON.put(elapsedMillis); 
										                 
								dataSet.put(dataJSON);
								
							}
							
						}
					}, 0, INTERVAL);
					startStop.getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
				}
				
				return running;
		
			} 
        	
        });
        
        //vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                      
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        
        //if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        	//mLocationManager.requestLocationUpdates(mLocationManager.getBestProvider(c, true), 0, 0, CarRampPhysics.this);
        
        accel       = new float[4];
        orientation = new float[3];
        rawAccel    = new float[3];
        rawMag      = new float[3];
        //loc         = new Location(mLocationManager.getBestProvider(c, true));
        
        mMediaPlayer = MediaPlayer.create(this, R.raw.beep); 
        
        if(firstName.length() == 0 || lastInitial.length() == 0)
        	showDialog(DIALOG_NEED_NAME);
        
        // Display the End User Agreement
        /*AlertDialog.Builder adb = new SimpleEula(this).show();
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
        }*/
        
    } 

	@Override
    public void onPause() {
    	super.onPause();
    	//mLocationManager.removeUpdates(CarRampPhysics.this);
    	mSensorManager.unregisterListener(CarRampPhysics.this);
    	if (timeTimer != null) timeTimer.cancel();
    	inPausedState = true;
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	//mLocationManager.removeUpdates(CarRampPhysics.this);
    	mSensorManager.unregisterListener(CarRampPhysics.this);
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
    		setupDone = false;
    		super.onBackPressed();	
    	}
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ITEM_ABOUT, Menu.NONE, "About");
		menu.add(Menu.NONE, MENU_ITEM_EULA,  Menu.NONE, "EULA" );
		return true;
	}
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (!useMenu) {
            menu.getItem(0).setEnabled(false);
            menu.getItem(1).setEnabled(false);
        } else {
            menu.getItem(0).setEnabled(true );
            menu.getItem(1).setEnabled(true );
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case MENU_ITEM_ABOUT:
        		showDialog(MENU_ITEM_ABOUT);
        		return true;
        	case MENU_ITEM_EULA:
        		showDialog(MENU_ITEM_EULA);
        		return true;
        }
        return false;
    }
    
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {	
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		DecimalFormat oneDigit = new DecimalFormat("#,##0.0");

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			rawAccel = event.values.clone();
			//accel[0] = event.values[0];
			accel[1] = event.values[1];
			accel[2] = event.values[2];
			
			//String xPrepend = accel[0] > 0 ? "+" : "";
			String yPrepend = accel[1] > 0 ? "+" : "";
			String zPrepend = accel[2] > 0 ? "+" : "";

			if (count == 0) {
				values.setText("Y: " + yPrepend + oneDigit.format(accel[1]) + 
						", Z: " + zPrepend + oneDigit.format(accel[2]));
			}
			
			//accel[3] = (float) Math.sqrt(Math.pow(accel[0], 2) + Math.pow(accel[1], 2) + Math.pow(accel[2], 2));

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
		//loc = location;
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
    	Dialog dialog;// = builder.setView(new View(this)).create();
    	
    	//dialog.show();
    	
    	WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
    	
	    switch(id) {
	    	
	    case DIALOG_CHOICE:
	    	
			builder.setTitle("Select An Action:")
	    	.setMessage("Would you like to upload your data to iSENSE or throw away this data set?")
	    	.setPositiveButton("iSENSE", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialoginterface,int i) {
	    			
	    			dialoginterface.dismiss();
	    			
	    			if(len == 0 || len2 == 0)
	    				Toast.makeText(CarRampPhysics.this, "There is no data to upload!", Toast.LENGTH_LONG).show();
	    			
	    			else {
			    		if(rapi.isConnectedToInternet())
			    			new Task().execute();
			    		else
			    			Toast.makeText(CarRampPhysics.this, 
			    					"No connection to Internet found!  Please enable wifi/mobile connectivity.", 
			    					Toast.LENGTH_LONG)
			    				 .show();
	    			}
	    		}
	    	})
			.setNegativeButton("Throw Away", new DialogInterface.OnClickListener() {
     		   	public void onClick(DialogInterface dialoginterface,int i) {
     		   		dialoginterface.dismiss();
     		   	} 
     	   	}) 
     	   .setCancelable(true);
	    	
	    	dialog = builder.create();
	    	
	    	break;
	    	
	    case DIALOG_FORCE_STOP:
	    	
	    	usedHomeButton = true;
	    	builder.setTitle("Data Recording Halted")
	    	.setMessage("You exited the app while data was still being recorded.  Data has stopped recording.")
	    	.setCancelable(false)
	    	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialoginterface, final int id) {
	            	   dialoginterface.dismiss();
	            	   startStop.performLongClick();
	            	   usedHomeButton = false;
	               }
	         });
	           
	    	dialog = builder.create();
	    
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
	    				"users to place the device down (in the position as instructed on the main screen of this app) " +
	    				"in a vehicle that is on an incline. " +
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
	    	
	    case MENU_ITEM_EULA:
	    	
	    	builder.setTitle("End User License Agreement")
	    	.setMessage(R.string.eula)
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
	    	dialog.show(); /* works but doesnt center it */
		    	
		   	lp.copyFrom(dialog.getWindow().getAttributes());
		   	lp.width = mwidth;
		   	lp.height = WindowManager.LayoutParams.MATCH_PARENT;
		   	lp.gravity = Gravity.CENTER_VERTICAL;
		   	lp.dimAmount=0.7f;
		   	
		   	dialog.getWindow().setAttributes(lp);
		   	dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
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
	    		
	    	if (dialog != null) {
		    	dialog.setOnDismissListener(new OnDismissListener() {
	            	@Override
	            	public void onDismiss(DialogInterface dialog) {
	            		removeDialog(id);
	            	}
	            });
		    }
		    
		    return dialog;
	    }
	    	    
	    
	}
    
    static int getApiLevel() {
    	return Integer.parseInt(android.os.Build.VERSION.SDK);
    }
    
    @Override  
    public void onActivityResult(int reqCode, int resultCode, Intent data) {  
        super.onActivityResult(reqCode, resultCode, data); 
    }

	public void getName(String name) {
		for(int i = 0; i < name.length(); i++) {
			if(name.charAt(i) != ' ') {
				firstName += name.charAt(i);
			} else {
				lastInitial = name.charAt(i + 1) + ".";
				break;
			}
		}
		Toast.makeText(CarRampPhysics.this, firstName + lastInitial, Toast.LENGTH_LONG).show();
	}
	
	private Runnable uploader = new Runnable() {
		
		@Override
		public void run() {
		
			int sessionId = -1;
			
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy, HH:mm:ss");
		    Date dt = new Date();
		    dateString = sdf.format(dt);
		    
			nameOfSession = firstName + " " +  lastInitial + ". - " + dateString;
			
			if(nameOfSession.equals("")) {
				sessionId = rapi.createSession(
						experimentNumber, 
						"*Session Name Not Provided*", 
						"Automated Submission Through Android App", 
						"N/A", "N/A", "United States");
			} else {
				sessionId = rapi.createSession(
						experimentNumber, 
						nameOfSession, 
						"Automated Submission Through Android App", 
						"N/A", "N/A", "United States");
			}
			
			sessionUrl = baseSessionUrl + sessionId;
			
			rapi.putSessionData(sessionId, experimentNumber, dataSet);
			
		}
		
	};

	private class Task extends AsyncTask <Void, Integer, Void> {
	    
	    @Override protected void onPreExecute() {
	    	
	        dia = new ProgressDialog(CarRampPhysics.this);
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
	        
	        len = 0; len2 = 0; mediaCount = 0;
	        
	        Toast.makeText(CarRampPhysics.this, "Data Has Uploaded Successfully", Toast.LENGTH_SHORT).show();
	        showDialog(DIALOG_VIEW_DATA);

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