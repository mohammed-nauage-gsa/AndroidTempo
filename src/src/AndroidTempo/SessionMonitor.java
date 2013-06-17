package src.AndroidTempo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import src.AndroidTempo.TEMPOService.LocalBinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This class is the activity that is used to SessionMonitor the state of the data
 * collection session and enter annotations.
 * 
 * @author Mohammed Nauage
 */
public class SessionMonitor extends Activity {

	private ArrayAdapter<TEMPODevice> devicesInSession;
	private TEMPOService mService;
	private DataOutputStream annoStream, timeStream;
	private ArrayList<String> annotationList;
	private PowerManager.WakeLock wakeLock;
	private boolean stopped;
	
	
	//0x00 waiting for mainTask to start collecting
	//0x01 mainTask collecting
	//0x02 waiting for mainTask to stop mainTask collecting
	//0x03 mainTask stopped collecting
	private byte hi;
	
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

		stopped = false;
		
		// creates files in temp direcotry to hold annotations and timestamps
		File annotations = new File(Main.tempDir, "Annotations.dat");
		File timestamps = new File(Main.tempDir, "Timestamps.dat");

		
		try {
			annoStream = new DataOutputStream(new FileOutputStream(annotations));
			timeStream = new DataOutputStream(new FileOutputStream(timestamps));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Intent connectionServiceIntent = new Intent(this,
				TEMPOService.class);
		startService(connectionServiceIntent);
		
		// bind to connection service
		// so functions in the service can be accessed
		connectionServiceIntent = new Intent(this,
				TEMPOService.class);
		bindService(connectionServiceIntent, mConnection,
				Context.BIND_AUTO_CREATE);

		// Makes new ListView
		ListView deviceList = (ListView) findViewById(R.id.deviceList);
		ArrayList<TEMPODevice> itemList = (ArrayList<TEMPODevice>) getIntent().getSerializableExtra("selected");
		
		devicesInSession = new ArrayAdapter<TEMPODevice>(deviceList.getContext(),
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
				TEMPOService.class));
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
	
		stopped = true;
		
		((Button)findViewById(R.id.stopSess)).setEnabled(false);
		((Button)findViewById(R.id.startAnno)).setEnabled(false);
		
		// stops collecting data and disconnects all nodes
		for(int i = 0; i < devicesInSession.getCount(); i++) {
			mService.stopCollection(devicesInSession.getItem(i));
		}
		// unbinds service
		unbindService(mConnection);
		// unregisters reciever

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


		}
		
		
		
	}
	
	private class mainTask extends AsyncTask<String,String,String>{
		
		private ArrayList<TEMPODevice> devicesCollecting;
		private File[] tempDataFile;
		private FileOutputStream[] tempDataFileWriter;
		private int[] currentFileNumber;
		
		@Override
		protected void onPreExecute() {
			devicesCollecting = mService.getNodesInSession();
			for(int i = 0; i < devicesInSession.getCount(); i++){
				if(!devicesCollecting.contains(devicesInSession.getItem(i))){
					devicesCollecting.remove(devicesInSession.getItem(i));
				}
			}
			currentFileNumber = new int[devicesCollecting.size()];
			tempDataFile = new File[devicesCollecting.size()];
			tempDataFileWriter = new FileOutputStream[devicesCollecting.size()];

			for(TEMPODevice i: devicesCollecting){
				makeNewTempFile(i);
			}

		}

		@Override
		protected String doInBackground(String... params) {
			byte[] buffer = new byte[4096];
			//ByteBuffer converter = ByteBuffer.allocate(4);
//			short expectedBytes = 0;
//			short bytesRead = 0;
//			short bytesAccepted = 0;
//			long timer = 0;
//			int currentSysCounter = 0;
//			int prevSysCounter = 0;
			TEMPOPacket[] newPackets = null;
			TEMPOPacket[] mostRecentPacket = null;
			
			while(!stopped){
				for(int i = 0; i < devicesCollecting.size(); i++){
					try {
						newPackets = devicesCollecting.get(i).getNewPackets(mostRecentPacket[i]);
					
						
						if(newPackets != null && newPackets.length > 0){
							if(mostRecentPacket == null)
								recordStartTime(newPackets[0].getTimeRecieved()-1000, devicesCollecting.get(i));
							mostRecentPacket[i] = newPackets[0];
						}
						
						for(int j = 0; j < newPackets.length; j++){
							buffer = newPackets[j].getByteBufferOfPacket().array();
							if(buffer != null && buffer.length > 6)
								tempDataFileWriter[i].write(buffer, 0, buffer.length-6);
						}
						
//						if(pipes.get(i).available() >= 2){
//							pipes.get(i).read(buffer, 0, 2);
//							expectedBytes = ByteBuffer.allocate(2).put(buffer, 0, 2).getShort();
//							timer = System.currentTimeMillis();
//							while(bytesRead < expectedBytes && System.currentTimeMillis() - timer < 2000){
//								bytesRead += pipes.get(i).read(buffer, bytesRead, expectedBytes - bytesRead);
//							}
//							if(bytesRead == expectedBytes){
//								currentSysCounter = TEMPOPacketUtil.getsysCounter(buffer);
//								bytesAccepted = validate(buffer, bytesRead, currentSysCounter, prevSysCounter);
//								prevSysCounter = currentSysCounter;
//								if(bytesAccepted > 6)
//									tempDataFileWriter[i].write(buffer, 0, bytesAccepted);
//							} else {
//								tempDataFileWriter[i].write(new byte[BYTES_PER_SAMPLE*SAMPLES_PER_SECOND*SECONDS_PER_POLL], 0, BYTES_PER_SAMPLE*SAMPLES_PER_SECOND*SECONDS_PER_POLL);							
//							}
//						}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
							
				}
			}
			
			stopped = true;
			
			return "Done!";
		}
		
		
//		private final static short BYTES_PER_SAMPLE = 12;
//		
//		private final static short SAMPLES_PER_SECOND = 128;

		private final static byte MOD_LF = '\n' + 0x10;
		
		private final static byte MOD_CR = '\r' + 0x10;
		
//		private final static byte SECONDS_PER_POLL = 1;

		private int balance;
		
		private short validate(byte[] buff, short end, int curSysCounter, int prevSysCounter){
			int diff;

			byte mcr = MOD_CR;
			byte mlf = MOD_LF;
			//Checks to see if it got the entire packet
			//and if it got the right number of bytes
			if (end <= 6 || buff[(end - 2)] != '\r' || buff[(end - 1)] != '\n'
					|| (end - 6) % TEMPODevice.BYTES_PER_SAMPLE != 0 || curSysCounter == 0 || prevSysCounter == 0) {
				Arrays.fill(buff, (byte) 0);
				return TEMPODevice.BYTES_EXPECTED_PER_POLL;//BYTES_PER_SAMPLE*SAMPLES_PER_SECOND*SECONDS_PER_POLL;

			} else {
				//makes sure that if 0x10 was added by the to prevent 
				//a byte from being a \r or \n it is set to the correct value
				for (int i = 0; i < end - 6; i += 2) {
					if (buff[i] == mlf || buff[i] == mcr) {
						buff[i] -= 0x10;
					}
					if (buff[i] >= 0x10) {
						Arrays.fill(buff, (byte) 0);
						return TEMPODevice.BYTES_EXPECTED_PER_POLL;
					}
				}
				
				//gets the difference in the expected number of 
				//data points recieved and the actual number
				diff = (int) (6 * (curSysCounter - prevSysCounter) - (end - 6));
				//adds diff to a varialbe which keeps track of 
				//the number of bytes the current data set is off by
				balance += diff;
				if (balance <= TEMPODevice.BYTES_PER_SAMPLE && balance >= -TEMPODevice.BYTES_PER_SAMPLE) {
					//if the number of bytes the current data set is 
					//off by is within acceptable ranges
					//it sets a variable to accept all that data from the packet
					diff = end - 6;


				} else if (balance > TEMPODevice.BYTES_PER_SAMPLE) {

					//If the data set has fewer points than allowable,
					//it pads the current data packet with 0's
					diff = (int) (TEMPODevice.BYTES_PER_SAMPLE * Math.floor(balance / TEMPODevice.BYTES_PER_SAMPLE));
					balance -= diff;
					diff += end - 6;

					for (int i = end - 6; i < diff; i++) {
						buff[i] = 0;
					}
				} else if (balance < -TEMPODevice.BYTES_PER_SAMPLE) {
					//If the data set has more points than allowable,
					//it only accepts a portion of the data.

					diff = (int) (TEMPODevice.BYTES_PER_SAMPLE * Math.ceil(balance / TEMPODevice.BYTES_PER_SAMPLE));
					balance -= diff;

					diff += end - 6;

				}
			}

			return (short) diff;

		}
		
		private void recordCalibData(TEMPODevice node){

			while(node.getCalibData() == null){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try {
				FileOutputStream calibFS = new FileOutputStream(new File(Main.tempDir, node.getMac()
						+ "_calib.dat"));

				byte[] calibData = node.getCalibData().array();
				calibFS.write(calibData, 0, calibData.length);


			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		private void recordStartTime(long l, TEMPODevice node){
						
			try {
				File f = new File(Main.tempDir, node.getMac()
						+ "_startTime.dat");
				if (!f.exists()) {
					DataOutputStream startTimeStream = new DataOutputStream(
							new FileOutputStream(f));
					startTimeStream.writeUTF(node.getName() + "\n");
					startTimeStream.writeLong(l);
					startTimeStream.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

		/**************************************************************************
		 * Makes a new temporary file to hold sensor data with properly formated name
		 *****************************************************************************/
		private void makeNewTempFile(TEMPODevice node) {
				int i = devicesCollecting.indexOf(node);
				currentFileNumber[i]++;
				tempDataFile[i] = new File(Main.tempDir, node.getMac() + "_" + currentFileNumber[i] + ".dat");
				try {
					tempDataFileWriter[i].close();
					tempDataFileWriter[i] = new FileOutputStream(tempDataFile[i], true);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			
		}
		
		@Override
		protected void onPostExecute(String result) {
			// ends the activity
			for(TEMPODevice i: devicesCollecting){
				recordCalibData(i);
			}
			
			new createXMLTask().execute();

			finish();

		}
		
		
		
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		/**************************************************************************
		 * Binds the TEMPOService service to this
		 * activity. It modifies the strings displayed to show some connection
		 * information, and starts the actual collection of data. Returns: void
		 *****************************************************************************/
		public void onServiceConnected(ComponentName className, IBinder service) {
			// binder used to get service
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			ArrayList<TEMPODevice> localDevicesInSession = new ArrayList<TEMPODevice>();
			
			for(int i = 0; i < devicesInSession.getCount(); i++){
				localDevicesInSession.add(devicesInSession.getItem(i));
			}

			for(TEMPODevice i: localDevicesInSession){
				mService.connectToDevice(i);
				mService.startCollection(i);

			}
//			nodesInSession = mService.getNodesConntected();
//
//
//			for(TEMPODevice i: nodesInSession)
//				mService.startCollection(i);
			
			
			long loopTimer = System.currentTimeMillis();
			while(!mService.getNodesInSession().contains(localDevicesInSession)){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(System.currentTimeMillis() - loopTimer > 5000)
					break;
				
			}
			
			new mainTask().execute();


		}

		public void onServiceDisconnected(ComponentName arg0) {

		}
	};
	
	
	
	/**************************************************************************
	 * This class keeps track of information concerning a ListView(what is
	 * or is not selected and what exists in a ListView).
	 *****************************************************************************/
	private class MonitorAdapter extends ArrayAdapter<TEMPODevice> {

		private final Context context;
		private ArrayList<TEMPODevice> vals;
		private ArrayList<Byte> states;
		/**************************************************************************
		 * Constructs an instance of SelectionAdapter that sets the values
		 * @param context The current context.
		 * @param pValues The list of strings that will initially be in the ListView.
		 *****************************************************************************/
		public MonitorAdapter(Context context, ArrayList<TEMPODevice> pValues) {
			super(context, R.layout.row, pValues);
			this.context = context;
			vals = pValues;
			states = new ArrayList<Byte>();
			for(int i = 0; i < vals.size(); i++)
				states.add((byte)0);
			

		}
		
		public void updateState(TEMPODevice node, byte state){
			states.set(vals.indexOf(node), state);
		}


		/**************************************************************************
		 * Get a View that displays the data at the 
		 * specified position in the data set.  It, also, specifies what the 
		 * checkbox in the item does.
		 * @param position The position of the item within the adapter's data set 
		 * 					of the item whose view we want.
		 * @param convertView The old view to reuse, if possible.
		 * @param parent The parent that this view will eventually be attached to
		 * @return A View corresponding to the data at the specified position.
		 *****************************************************************************/
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View rowView = inflater.inflate(R.layout.row, parent, false);
			TextView textView = (TextView) rowView.findViewById(R.id.label);
			textView.setText(vals.get(position).toString());
			
			
			
			switch(states.get(position)){
				case 0:
					textView.setTextColor(Color.GREEN);
					break;
				case 1:
					textView.setTextColor(Color.YELLOW);
					break;
				case 2:
					textView.setTextColor(Color.RED);
					break;
			}
			

			return rowView;
		}
		
		

	}


}
