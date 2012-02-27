package edu.uml.cs.isense.amusement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import edu.uml.cs.isense.comm.RestAPI;
import edu.uml.cs.isense.amusement.R;

public class LoginActivity {	
	private RestAPI rapi;
	private Context mContext;
	
	static final public int LOGIN_SUCCESSFULL = 1;
	static final public int LOGIN_FAILED = 0;
	static final public int LOGIN_CANCELED = -1;
	
	boolean success;
	
	private String message = "";
	/* { */
		private static final String unknownUser    = "Connection to internet has been found, but the username or password was incorrect.  Please try again.";
		private static final String noConnection   = "No connection to internet through either wifi or mobile found.  Please enable one to continue, then try again."; 
		private static final String defaultMessage = "Was your username and password correct?\nAre you connected to the internet?\nPlease try again.";
		//private static final String error600       = "Our servers are busy right now and cannot log you in (http response code 600).";
	/* } */
	
	@SuppressWarnings("unused")
		private SharedPreferences settings;
	
	public LoginActivity(Context c) {
		mContext = c;
		rapi = RestAPI.getInstance();
		
		settings = PreferenceManager.getDefaultSharedPreferences(mContext);
   	}

	public AlertDialog getDialog(final Handler h) {
		return getDialog(h, "");
	}
	
	public AlertDialog getDialog(final Handler h, final String message) {
		
			final Message loginSuccess = Message.obtain();
			loginSuccess.setTarget(h);
			loginSuccess.what = LOGIN_SUCCESSFULL;
			
			final Message rejectMsg = Message.obtain();
			rejectMsg.setTarget(h);
			rejectMsg.what = LOGIN_CANCELED;
			
			final View v;
			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.logindialog, null);
			
            final EditText usernameInput = (EditText) v.findViewById(R.id.usernameInput);
			final EditText passwordInput = (EditText) v.findViewById(R.id.passwordInput);
			
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            
            builder.setView(v);
            
            builder.setMessage(message)
            	   .setPositiveButton("Login", new DialogInterface.OnClickListener() {
            		   public void onClick(DialogInterface dialog, int id) {
            			   success = rapi.login(usernameInput.getText().toString(), passwordInput.getText().toString());
            			   Log.w("CNCTN", "user: " + usernameInput.getText().toString());
            			   Log.w("CNCTN", "pass: " + passwordInput.getText().toString());
            			               			               			   
            			   if (success) {
            				   AmusementPark.loginName = usernameInput.getText().toString();
            				   loginSuccess.sendToTarget();
            				   dialog.dismiss();
            			   } else {
            				   showFailure(h);
            				   dialog.dismiss();
            			   }
            			   dialog.dismiss();
            		   }
            	   })
            	   .setCancelable(true);
            	   
             
            	return builder.create();
	}
    
	private void showFailure(Handler h) {
		final Message msg = Message.obtain();
		msg.setTarget(h);
		msg.what = LOGIN_FAILED;
		Log.e("CNCTN", "connection: " + rapi.connection);
		if(rapi.connection == "NONE") message = noConnection;
		else if(rapi.connection == "600") message = unknownUser;
		else message = defaultMessage;
			
		new AlertDialog.Builder(mContext)
			.setTitle("Login Failed")
			.setMessage(message)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					msg.sendToTarget();
				}
			})
			.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					msg.sendToTarget();
				}
			})
			.show();
	}
	
}

