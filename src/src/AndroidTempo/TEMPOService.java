package src.AndroidTempo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

/**
 * This class is used to manage Bluetooth connections and collect data.
 * 
 * @author Mohammed Nauage
 */
public class TEMPOService extends Service {

	
	/** Directory in which all the data concerning this Application will be held. 
	*/
	public final static File rootDir = new File(
			Environment.getExternalStorageDirectory(), "Android Tempo");
	/** Directory where all the temporary files for the session will exist.
	*/
	public static File tempDir = new File(rootDir, "temp");
	
	/** Signifies a session was properly started
	*/
    public static final byte SESSION_STARTED_PROPERLY = 0x00;

	/** Signifies in the processes of starting a session there
	 * existed devices which could not be discovered
	*/
    public static final byte UNABLE_TO_DISCOVER_ALL_DEVICES = 0x01;
    
	/** Signifies in the processes of starting a session there
	 * existed devices to which a connection could not be established
	*/
    public static final byte UNABLE_TO_CONNECT_TO_ALL_DEVICES = 0x02;
    
	/** Signifies in the processes of starting a session there
	 * existed devices with which the application was unable to communicate
	*/
    public static final byte UNABLE_TO_COMMUNICATE_WITH_ALL_DEVICES = 0x04;
    
	public static final int REQUEST_ENABLE_BT = 1;

    

	private final IBinder binder = new LocalBinder();
	private ArrayList<TEMPODevice> nodesInSession = new ArrayList<TEMPODevice>();
	private ArrayList<TEMPODevice> nodesConnected = new ArrayList<TEMPODevice>();
	private ArrayList<TEMPODevice> availableNodes = new ArrayList<TEMPODevice>();
	private static Hashtable<String, TEMPODevice> nodes = new Hashtable<String, TEMPODevice>();

    

    
	/**************************************************************************
	 * Returns the nodes that are currently in session
	 * @return An array of nodes that are currently in session
	 *****************************************************************************/
    public ArrayList<TEMPODevice> getNodesInSession(){
    	
    	for(TEMPODevice i: nodesInSession){
    		if(!i.isInSession()){
    			nodesInSession.remove(i);
    		}
    	}
    	
    	return nodesInSession;
    }
    
    public ArrayList<TEMPODevice> getNodesConntected(){
    	
    	for(TEMPODevice i: nodesConnected){
    		if(!i.isConnected()){
    			nodesConnected.remove(i);
    		}
    	}
    	
    	return nodesConnected;
    }

    /**************************************************************************
     * Default binder description for this service
     *****************************************************************************/
	public class LocalBinder extends Binder {
		public TEMPOService getService() {
			return TEMPOService.this;
		}
	}


	/**************************************************************************
	 * Returns the communication channel to the service.
	 * @param intent The Intent that was used to bind to this service, 
	 * 					as given to Context.bindService. 
	 * @return Return an IBinder through which clients can call on to the service.
	 *****************************************************************************/
	@Override
	public IBinder onBind(Intent intent) {
		// make toast to confirm binding
		if(!rootDir.exists()){
			rootDir.mkdir();
		}
		

		Toast.makeText(this, "Service Bound", Toast.LENGTH_LONG).show();
		return binder;
	}

	

	/**************************************************************************
	 * Prepares the service.
	 * @param intent The Intent supplied to startService(Intent), as given.
	 * @param flags Additional data about this start request. Currently either 
	 * 				0, START_FLAG_REDELIVERY, or START_FLAG_RETRY.
	 * @param startId A unique integer representing this specific request to start.
	 * @return The return value indicates what semantics the system should use 
	 * 			for the service's current started state.
	 *****************************************************************************/
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		
		//Causes the service to run in the foreground decreasing its porbability of being
		//stopped due to low resources
		Notification notification = new Notification();
		Intent notificationIntent = new Intent(this, TEMPOService.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this,
				"Android Tempo Connection Service", "", pendingIntent);
		startForeground(1234, notification);
		registerReceiver(Receiver, new IntentFilter(
				BluetoothDevice.ACTION_FOUND));
		
		
		createDefaultFiles();
		getNodes();
		
		return START_NOT_STICKY;
	}
		
	
	/**************************************************************************
	 * Gets the node MACs and names from a file and stores it in a Hashtable.
	 *****************************************************************************/
	private void getNodes() {

		

		// Once the file exists and is properly up to date, it fills a HashTable
		// with the names and MACs.
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(rootDir, "devices.csv"))));
			String s;
			while (reader.ready()) {
				s = reader.readLine();
				String[] sa = s.split(",");
				nodes.put(sa[0].toUpperCase(), new TEMPODevice(sa[0].toUpperCase(), sa[1]));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	/**************************************************************************
	 * Creates default files and folders if they do not exist
	 *****************************************************************************/
	private void createDefaultFiles(){
		File devicesCsv;
		BufferedReader reader = null;
		FileWriter writer = null;
		
		// Checks if the directory in which all the ProgramState regarding this
		// application will be saved exists. If it does not, it creates the
		// directory.
		if (!rootDir.exists() || !rootDir.isDirectory()) {
			rootDir.mkdir();
		}
		
		
		devicesCsv = new File(rootDir, "devices.csv");
		
		// If devices.csv does not already exist, then it creates a new one
		// using the device.csv in assets.
		if (!devicesCsv.exists()) {
			try {
				reader = new BufferedReader(new InputStreamReader(this
						.getAssets().open(getString(R.string.devices_csv))));
				writer = new FileWriter(devicesCsv);
			} catch (IOException e) {
			}

			try {
				while (reader.ready()) {
					writer.write(reader.readLine() + "\n");
				}
				writer.close();
				reader.close();
			} catch (IOException e) {
			}

		}

	}
	
	public ArrayList<TEMPODevice> getAvailableNodes(){

		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		btAdapter.cancelDiscovery();
		btAdapter.startDiscovery();
		
		while(btAdapter.isDiscovering()){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		return availableNodes;
		
		
		
	}
	
	
	/**************************************************************************
	 * Checks to see if Bluetooth is supported, and if
	 * Bluetooth is not enabled it enables it.
	 *****************************************************************************/
	public byte checkBlueToothState() {
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			//return not supported
			return 0x12;
		} else {
			if (btAdapter.isEnabled()) {
				// OK
				return 0x10;
			} else {
				// Starts the bluetooth enabling process if user has bluetooth
				// but does not have it enabled
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(enableBtIntent);
				return 0x11;
			}
		}
	}
	
	private final BroadcastReceiver Receiver = new BroadcastReceiver() {

		/**************************************************************************
		 * When a Bluetooth device is found, it creates a string containing the 
		 * name and MAC of the device if it is a TEMPO node, and it adds it to a view
		 * to display it.
		 * @param context The Context in which the receiver is running.
		 * @param intent The Intent being received.
		 *****************************************************************************/
		@Override
		public void onReceive(Context context, Intent intent) {
			if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
				// gets the device found
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// gets the MAC of the device found
				String macAddress = device.getAddress().toUpperCase();

				if (nodes.get(macAddress) != null && !availableNodes.contains(macAddress)) {
					availableNodes.add(nodes.get(macAddress));
				}
			}
		}
	};

	
	

	/**************************************************************************
	 * Attempts to establish Bluetooth connection with the given TEMPO node
	 * @param node A TEMPODevice object which contains information concerning
	 * 				the node to which you want to connect.
	 * @return True if connection was established and false if it failed
	 *****************************************************************************/
	public synchronized boolean connectToDevice(TEMPODevice node) {
		if (node != null && nodesConnected.size() < 7){
			if (node.connect() && !nodesConnected.contains(node))
				nodesConnected.add(node);
			
			return node.isConnected();
		}
		return false;
	}
 
	/**************************************************************************
	 * Attempts to disconnect from the given device
	 * @param node A TEMPODevice object which contains information concerning
	 * 				the node from which you want to disconnect.
	 * @return True if disconnection was successful else false
	 *****************************************************************************/
	public synchronized boolean disconnectFromDevice(TEMPODevice node) {
		if(node != null) {
			if(node.disconnect() && nodesConnected.contains(node)){
				nodesConnected.remove(node.getMac());
			}
			
			return !node.isConnected();
			
		}
		return false;
	}

		
	public synchronized boolean startCollection(TEMPODevice node){
		
		if(nodesConnected.contains(node)){
			node.start();
			nodesInSession.add(node);
			return true;
		} else {
			return false;
		}
		
	}
	

	/**************************************************************************
	 * Ends the session by disconnecting all nodes and clears all fields
	 * @return True it successfully disconnected from all nodes and cleared 
	 * 			all fields else false
	 *****************************************************************************/
	public synchronized boolean stopCollection(TEMPODevice node) {
		if(node != null){
			node.cancel();
			try {
				node.join();
				nodesInSession.remove(node);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return true;
		} else {
			return false;	
		}

	}

	public TEMPODevice getTEMPODevice(String mac) {
		// TODO Auto-generated method stub
		return nodes.get(mac.toUpperCase());
	}
	
}
