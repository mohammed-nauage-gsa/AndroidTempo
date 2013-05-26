package src.AndroidTempo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import src.AndroidTempo.ConnectionService.LocalBinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

/**
 * This class is the activity that is used to SessionMonitor the state of the data
 * collection session and enter annotations.
 * 
 * @author Mohammed Nauage
 */
public class SessionMonitor extends Activity {

	private ArrayAdapter<String> devicesInSession;
	private ConnectionService mService;
	private DataOutputStream annoStream, timeStream;
	private ArrayList<String> annotationList;
	private PowerManager.WakeLock wakeLock;

	/**************************************************************************
	 * Initializes activity.
	 * @param savedInstanceState holds all the data that may be saved when
	 * 							onSaveInstanceState
	 *****************************************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.collect);

		// creates files in temp direcotry to hold annotations and timestamps
		File annotations = new File(Main.tempDir,
				"Annotations.dat");
		File timestamps = new File(Main.tempDir, "Timestamps.dat");

		try {
			annoStream = new DataOutputStream(new FileOutputStream(annotations));
			timeStream = new DataOutputStream(new FileOutputStream(timestamps));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Intent connectionServiceIntent = new Intent(this,
				ConnectionService.class);
		startService(connectionServiceIntent);
		
		// bind to connection service
		// so functions in the service can be accessed
		connectionServiceIntent = new Intent(this,
				ConnectionService.class);
		bindService(connectionServiceIntent, mConnection,
				Context.BIND_AUTO_CREATE);

		// Makes new ListView
		ListView deviceList = (ListView) findViewById(R.id.deviceList);
		ArrayList<String> itemList = (ArrayList<String>) getIntent().getSerializableExtra("selected");
		for(int i = 0; i < itemList.size(); i++){
			itemList.set(i, itemList.get(i) == "" ? "<location>": itemList.get(i) + "\n0/0");
		}
		devicesInSession = new ArrayAdapter<String>(deviceList.getContext(),
				R.layout.basic_row, itemList);
		deviceList.setAdapter(devicesInSession);

		
		annotationList = (ArrayList<String>)(getIntent().getSerializableExtra("annotations"));
		
		PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,"Wake Lock");
		wakeLock.acquire();

	}

	/**************************************************************************
	 * Deallocates resources for this activity.
	 *****************************************************************************/
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.stopService(new Intent(SessionMonitor.this,
				ConnectionService.class));
		wakeLock.release();

	}

	/**************************************************************************
	 * Overrides the functionality of the back button to return to the
	 * screen prior to the starting of the app. 
	 * @param keyCode A key code that represents the button pressed, from KeyEvent.
	 * @param event The KeyEvent object that defines the button action.
	 * @return If you handled the event, return true. If you want to allow the event 
	 * 			to be handled by the next receiver, return false.
	 *****************************************************************************/
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**************************************************************************
	 * Prompts the user for an annotation, and saves the annotation and
	 * the time at which the annotation was entered to a file.
	 * @param view The view that was clicked 
	 *****************************************************************************/
	public void onAnnotationClick(View arg0) {
		// saves the time when the annotation is being set
		try {
			timeStream.writeLong(System.currentTimeMillis());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// creates alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(SessionMonitor.this);
		ArrayAdapter<String> adapter = 
				new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,annotationList);
		final InstantAutoComplete input = new InstantAutoComplete(SessionMonitor.this);		
		input.setAdapter(adapter);
		
		
		input.setOnKeyListener(new OnKeyListener(){

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// do nothing
				if(keyCode == KeyEvent.KEYCODE_ENTER);
				return false;
			}
			
		});
		
		alert.setView(input);

		alert.setMessage("Enter Annotation: ");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

				try {
					// takes text from input box and writes it to a
					// file
					String inputString = input.getText().toString();
					annoStream.writeBytes(inputString + ",");
					if(!annotationList.contains(inputString)){
						annotationList.add(inputString);
						Collections.sort(annotationList);
					}
					
				} catch (IOException e) {
				}

			}
		});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		
		alert.create().show();

	}

	/**************************************************************************
	 * Ends the current data collection session and activity.
	 * @param view The view that was clicked
	 *****************************************************************************/
	public void onStopSessionClick(View arg0) {
	
		((Button)findViewById(R.id.stopSess)).setEnabled(false);
		((Button)findViewById(R.id.startAnno)).setEnabled(false);
		
		// stops collecting data and disconnects all nodes
		mService.stopSession();
		// unbinds service
		unbindService(mConnection);
		// unregisters reciever
		new createXMLTask().execute();

	}
	
	
	private class createXMLTask extends AsyncTask<String,String,String>{
		@Override
		protected void onPreExecute() {
		}

		@Override
		protected String doInBackground(String... params) {			

			
	        SessionMonitor.this.runOnUiThread(new Runnable() {

	            public void run(){
	    			XMLGenerator xmlWriter;
	    			try {
	    				// creates datToXML to convert everything to the XML using the
	    				// template
	    				xmlWriter = new XMLGenerator(Main.tempDir, getAssets()
	    						.open(XMLGenerator.template));
	    				// starts the process
	    				xmlWriter.generateXML();

	    			} catch (Exception e) {
	    				// TODO Auto-generated catch block
	    				e.printStackTrace();
	    			}
	            }
	        });
			return "Done!";
		}
		
		@Override
		protected void onPostExecute(String result) {
			// ends the activity

			finish();

		}
		
		
		
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		/**************************************************************************
		 * Binds the ConnectionService service to this
		 * activity. It modifies the strings displayed to show some connection
		 * information, and starts the actual collection of data. Returns: void
		 *****************************************************************************/
		public void onServiceConnected(ComponentName className, IBinder service) {
			// binder used to get service
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mService.setHandler(mHandler);

			ArrayAdapter<String> adpt = devicesInSession;

			String item;
			String[] itemParts;

			for (int i = 0; i < adpt.getCount(); i++) {
				item = (String) adpt.getItem(i);
				itemParts = item.split("\n");
				mService.addDeviceToSessionList(itemParts[1], itemParts[0]);
			}
			
			// Connects the device and if it was able to connect it prepares
			// them for data collection
			mService.prepareAllNodesForSession();

			// starts data collection for all the nodes connected
			
			mService.startSession();

			adpt.notifyDataSetChanged();

			// waits to make sure all nodes have started collecting
			// so user does not attempt to annotate at a time when they aren't
			// all collecting
			// causing the timestamp to potentially be negative
			// since they are synced to some time after the latest time a node
			// has been polled first
			while (!mService.allNodesCollecting()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		public void onServiceDisconnected(ComponentName arg0) {

		}
	};

	/**************************************************************************
	 * Handles messages received from ConnectionService and updates the UI accordingly.
	 *****************************************************************************/	
	public final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				ArrayAdapter<String> adpt = devicesInSession;
				String message = (String) msg.obj;
				String[] messageParts = message.split(",");
				for (int i = 0; i < adpt.getCount(); i++) {
					if (adpt.getItem(i).contains(messageParts[0])) {
						String item = adpt.getItem(i);
						adpt.remove(item);
						String[] itemParts = item.split("\n");
						adpt.insert(itemParts[0] + "\n" + itemParts[1] + "\n" + itemParts[2] + "\n"
								+ messageParts[1], i);
						adpt.notifyDataSetChanged();
						break;
					}
				}
				break;
			}
		}
	};

}
