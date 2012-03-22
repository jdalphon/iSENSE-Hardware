package edu.uml.cs.isense.carphysics;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

public class LoginActivity {	
	//private RestAPI rapi;
	private Context mContext;
	
	static final public int NAME_SUCCESSFULL = 1;
	static final public int NAME_FAILED = 0;
	static final public int NAME_CANCELED = -1;
	
	boolean success;
	
	private String message = "";
	private static final String blankFields = "Do not leave any fields blank.  Please enter your first name and last initial.";
		
	
	@SuppressWarnings("unused")
		private SharedPreferences settings;
	
	public LoginActivity(Context c) {
		mContext = c;
		//rapi = RestAPI.getInstance();
		
		settings = PreferenceManager.getDefaultSharedPreferences(mContext);
   	}

	public AlertDialog getDialog(final Handler h) {
		return getDialog(h, "");
	}
	
	public AlertDialog getDialog(final Handler h, final String message) {
		
			final Message loginSuccess = Message.obtain();
			loginSuccess.setTarget(h);
			loginSuccess.what = NAME_SUCCESSFULL;
			
			final Message rejectMsg = Message.obtain();
			rejectMsg.setTarget(h);
			rejectMsg.what = NAME_CANCELED;
			
			final View v;
			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.entername, null);
			
            final EditText firstNameInput   = (EditText) v.findViewById(R.id.nameInput);
			final EditText lastInitialInput = (EditText) v.findViewById(R.id.initialInput);
			
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            
            builder.setView(v);
            
            builder.setMessage(message)
            	   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            		   @Override
            		   public void onClick(DialogInterface dialog, int id) {
            			   
            			   if(firstNameInput.length() == 0 || lastInitialInput.length() == 0) {
            				   dialog.dismiss();
            				   showFailure(h);
            				   //CarRampPhysics.nameSuccess = false;
            			   } else {
            				   //CarRampPhysics.nameSuccess = true;
            				   CarRampPhysics.firstName   = firstNameInput.getText().toString();
            				   CarRampPhysics.lastInitial = lastInitialInput.getText().toString();
            				   loginSuccess.sendToTarget();
            				   dialog.dismiss();
            			   }
            			   dialog.dismiss();
            		   }
            	   })
            	   .setCancelable(false);
            	   
             
            	return builder.create();
	}
    
	private void showFailure(Handler h) {
		final Message msg = Message.obtain();
		msg.setTarget(h);
		msg.what = NAME_FAILED;
		message = blankFields;
			
		new AlertDialog.Builder(mContext)
			.setTitle("Error")
			.setMessage(message)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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


