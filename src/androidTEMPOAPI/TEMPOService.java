package androidTEMPOAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.UUID;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
	private ArrayList<TEMPODeviceInfo> nodesInSession = new ArrayList<TEMPODeviceInfo>();
	private ArrayList<TEMPODeviceInfo> nodesConnected = new ArrayList<TEMPODeviceInfo>();
	private ArrayList<TEMPODeviceInfo> availableNodes = new ArrayList<TEMPODeviceInfo>();
	private static Hashtable<String, TEMPODevice> nodes = new Hashtable<String, TEMPODevice>();

    

    
	/**************************************************************************
	 * Returns the nodes that are currently in session
	 * @return An array of nodes that are currently in session
	 *****************************************************************************/
    public ArrayList<TEMPODeviceInfo> getNodesInSession(){
    	
    	for(TEMPODeviceInfo i: nodesInSession){
    		if(!nodes.get(i.getMac()).isInSession()){
    			nodesInSession.remove(i);
    		}
    	}
    	
    	return nodesInSession;
    }
    
    public ArrayList<TEMPODeviceInfo> getNodesConntected(){
 
    	for(TEMPODeviceInfo i: nodesConnected){
    		if(!nodes.get(i.getMac()).isConnected()){
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
	
	public ArrayList<TEMPODeviceInfo> getAvailableNodes(){

	   	ArrayList<TEMPODeviceInfo> ret = new ArrayList<TEMPODeviceInfo>();
	    
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
					availableNodes.add(nodes.get(macAddress).getTEMPODeviceInfo());
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
	public synchronized boolean connectToDevice(TEMPODeviceInfo deviceDescriptor) {
		if (deviceDescriptor != null && nodesConnected.size() < 7){
			TEMPODevice node = nodes.get(deviceDescriptor.getMac());
			if (node != null && node.connect() && !nodesConnected.contains(deviceDescriptor))
				nodesConnected.add(deviceDescriptor);
			
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
	public synchronized boolean disconnectFromDevice(TEMPODeviceInfo deviceDescriptor) {
		if(deviceDescriptor != null) {
			
			TEMPODevice node = nodes.get(deviceDescriptor.getMac());
			if(node != null && node.disconnect() && nodesConnected.contains(deviceDescriptor)){
				nodesConnected.remove(deviceDescriptor);
			}
			
			return !node.isConnected();
			
		}
		return false;
	}

		
	public synchronized boolean startCollection(TEMPODeviceInfo deviceDescriptor){
		
		if(nodesConnected.contains(deviceDescriptor)){
			TEMPODevice node = nodes.get(deviceDescriptor.getMac());
			if(node != null) {
				node.start();
				nodesInSession.add(deviceDescriptor);
				return true;
			}
		}
		
		return false;
		
		
	}
	

	/**************************************************************************
	 * Ends the session by disconnecting all nodes and clears all fields
	 * @return True it successfully disconnected from all nodes and cleared 
	 * 			all fields else false
	 *****************************************************************************/
	public synchronized boolean stopCollection(TEMPODeviceInfo deviceDescriptor) {
		if(deviceDescriptor != null){
			TEMPODevice node = nodes.get(deviceDescriptor.getMac());
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
			}
			
		}
		return false;	
		

	}
	
	public ByteBuffer getCalibData(TEMPODeviceInfo deviceDescriptor){
		TEMPODevice device = nodes.get(deviceDescriptor.getMac());
		
		if(device != null)
			return device.getCalibData();
		else 
			return null;

	}
	
	public int getMaxPacketsStored(TEMPODeviceInfo deviceDescriptor){
		
		TEMPODevice device = nodes.get(deviceDescriptor.getMac());
		
		if(device != null)
			return device.getMaxPacketsStored();
		else 
			return 0;
	
	}
	
	public boolean dataIsAvailable(TEMPODeviceInfo deviceDescriptor) {
		TEMPODevice device = nodes.get(deviceDescriptor.getMac());
		
		if(device != null)
			return device.dataIsAvailable();
		else 
			return false;
	}
	
	public boolean dataIsAvailableSincePacket(TEMPODeviceInfo deviceDescriptor, TEMPOPacket packet) {
		TEMPODevice device = nodes.get(deviceDescriptor.getMac());
		
		if(device != null)
			return device.dataIsAvailableSincePacket(packet);
		else 
			return false;
	}
	
	public TEMPOPacket[] getRawDataSincePacket(TEMPODeviceInfo deviceDescriptor, TEMPOPacket packet) {
		TEMPODevice device = nodes.get(deviceDescriptor.getMac());
		
		if(device != null)
			return device.getRawDataSincePacket(packet);
		else 
			return null;
	}
	
	public TEMPOPacket[] getValidatedDataSincePacket(TEMPODeviceInfo deviceDescriptor, TEMPOPacket packet) {
		TEMPODevice device = nodes.get(deviceDescriptor.getMac());
		
		if(device != null)
			return device.getValidatedDataSincePacket(packet);
		else 
			return null;
	}	
		
	private class TEMPODevice extends Thread{

		/** Serial Port Profile Universal Unique Identifier
		*/
		public final static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
		
		public final static float SECONDS_PER_POLL = 1f;
		
		public final static int BYTES_PER_SAMPLE = 12;
		
		public final static int SAMPLES_PER_SECOND = 128;
		
		public final static int BYTES_EXPECTED_PER_POLL = (int) (SECONDS_PER_POLL * BYTES_PER_SAMPLE * SAMPLES_PER_SECOND + 6); 
		
		private final static int PACKET_BUFFER_SIZE = 5;
		
		private final static int IO_BUFFER_SIZE = 5*BYTES_EXPECTED_PER_POLL;

		
		private boolean inSession;
		private boolean connected;
		private BluetoothDevice device;
		private BluetoothSocket socket;
		private TEMPOPacketBuffer rawPacketBuffer;
		private TEMPOPacketBuffer validatedPacketBuffer;
		private ByteBuffer calib;
		private TEMPODeviceInfo info;
		
		

		/**************************************************************************
		 * Class constructor
		 *****************************************************************************/		
		public TEMPODevice(String mac, String name) {
			device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac.toUpperCase());
			connected = false;
			inSession = false;
			socket = null;
			info = new TEMPODeviceInfo(mac, name);
			rawPacketBuffer = new TEMPOPacketBuffer(PACKET_BUFFER_SIZE);

	 
		}
		

		public TEMPODeviceInfo getTEMPODeviceInfo() {
			// TODO Auto-generated method stub
			return null;
		}


		/**************************************************************************
		 * MAC getter
		 * @return the MAC value
		 *****************************************************************************/
		public String getMac() {
			return info.getMac();
		}

		/**************************************************************************
		 * connected getter
		 * @return the connected value
		 *****************************************************************************/
		public boolean isConnected() {
			if (socket == null)
				return false;

			return connected;
		}

		/**************************************************************************
		 * inSession getter
		 * @return the inSession value
		 *****************************************************************************/
		public boolean isInSession() {
			return inSession || this.isAlive();
		}


		/**************************************************************************
		 * Establishes a connection with the device
		 * @return If the connection was established properly, it returns true.
		 * 			Otherwise, it returns false.
		 *****************************************************************************/
		public boolean connect() {


		
			try {
				socket = device.createRfcommSocketToServiceRecord(UUID
						.fromString(SPP_UUID));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
				//}


			if (socket != null) {
				// stops discovery
				BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

				// calls default BluetoothDevice function to connect and sets a
				// boolean
				// which states whether a device is connected or not
				try {
					socket.connect();
					connected = true;

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					try {
						socket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
						
				}
			}
			
			return connected;
		}


		/**************************************************************************
		 * Closes Bluetooth connection
		 * @return If the connection was closed properly, it returns true. Otherwise,
		 * 			it returns false.
		 *****************************************************************************/
		public boolean disconnect() {
			try {
				socket.close();
				connected = false;
			} catch (IOException e) {

			}
			return !connected;
		}
		

		/**************************************************************************
		 * Indicates whether some other device is "equal to" this one.
		 * @param device other device
		 * @return If the the devices are the same, it returns true. Otherwise,
		 * 			it returns false.
		 *****************************************************************************/
		@Override
		public boolean equals(Object o) {
			if(o == null || !(o instanceof TEMPODevice))
				return false;

			if(((TEMPODevice)o).getMac().equals(getMac()))
				return true;
			

			return false;
		}
		


		public int getMaxPacketsStored(){
			return rawPacketBuffer.getSize();
		}

//		public TEMPOPacket[] getLastKPackets(int k){
//			return rawPacketBuffer.getLastKPackets(k);		
//		}
//		
//		public TEMPOPacket[] getAllPackets(){
//			return rawPacketBuffer.getLastKPackets(rawPacketBuffer.getSize());
//		}
//		
//		public TEMPOPacket[] getNewPackets(TEMPOPacket lastPacketRead) {
//			TEMPOPacket[] packetsAvailable = rawPacketBuffer.getLastKPackets(PACKET_BUFFER_SIZE);
//			ArrayList<TEMPOPacket> ret = new ArrayList<TEMPOPacket>();
//			if(lastPacketRead != null){
//				for(TEMPOPacket i: packetsAvailable){
//					if(i != null && i.getTimeRecieved() > lastPacketRead.getTimeRecieved()){
//						ret.add(i);
//					}
//				}
//			} else {
//				for(TEMPOPacket i: packetsAvailable){
//					if(i != null){
//						ret.add(i);
//					}
//				}			
//			}
//			return (TEMPOPacket[]) ret.toArray();
//		}
//		
//		public long getTimeLastPacketRecieved(){
//			
//			return rawPacketBuffer.getTimeLastPacketRecieved();
//		}
		
		
		public boolean dataIsAvailable() {
			return rawPacketBuffer.getLastKPackets(1)[0] != null;
		}
		
		public boolean dataIsAvailableSincePacket(TEMPOPacket packet) {
			TEMPOPacket mostRecentPacket = rawPacketBuffer.getLastKPackets(1)[0];
			if(packet != null){				
				return mostRecentPacket.getTimeRecieved() > packet.getTimeRecieved();
			} else {
				return mostRecentPacket != null;
			}
		}
		
		public TEMPOPacket[] getRawDataSincePacket(TEMPOPacket packet) {
			TEMPOPacket[] packetsAvailable = rawPacketBuffer.getLastKPackets(PACKET_BUFFER_SIZE);
			ArrayList<TEMPOPacket> ret = new ArrayList<TEMPOPacket>();
			if(packet != null){
				for(TEMPOPacket i: packetsAvailable){
					if(i != null && i.getTimeRecieved() > packet.getTimeRecieved()){
						ret.add(i);
					}
				}
			} else {
				for(TEMPOPacket i: packetsAvailable){
					if(i != null){
						ret.add(i);
					}
				}			
			}
			return (TEMPOPacket[]) ret.toArray();
		
		}
		
		public TEMPOPacket[] getValidatedDataSincePacket(TEMPOPacket packet) {
			TEMPOPacket[] packetsAvailable = validatedPacketBuffer.getLastKPackets(PACKET_BUFFER_SIZE);
			ArrayList<TEMPOPacket> ret = new ArrayList<TEMPOPacket>();
			if(packet != null){
				for(TEMPOPacket i: packetsAvailable){
					if(i != null && i.getTimeRecieved() > packet.getTimeRecieved()){
						ret.add(i);
					}
				}
			} else {
				for(TEMPOPacket i: packetsAvailable){
					if(i != null){
						ret.add(i);
					}
				}			
			}
			return (TEMPOPacket[]) ret.toArray();

		}	
		
		
		public ByteBuffer getCalibData(){
			if(!isInSession() && connected)
				getCalibDataFromNode();

			if(calib == null)
				return null;
			return cloneByteBuffer(calib);

		}


		
		/**************************************************************************
		 * Main loop for polling the node and collecting data
		 *****************************************************************************/
		public void run() {

			// initializes variables
			inSession = true;

			long time = 0;
			short bytes = 0;
			InputStream i = null;
			OutputStream o = null;
			byte[] buffer = new byte[(int) (IO_BUFFER_SIZE)];
			
			
			try {
				i = socket.getInputStream();
				o = socket.getOutputStream();
				// gets the clock and time in milliseconds
				time = initSession();

				o.write(TEMPOCommands.SEND2);

			} catch (IOException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}


			// main loop
			while (inSession) {
				

				try {

					if (System.currentTimeMillis() - time < (long) SECONDS_PER_POLL * 1000) {
						Thread.sleep((long) (SECONDS_PER_POLL * 1000)
								- (System.currentTimeMillis() - time));
					}

					if (this.isConnected()) {
						bytes = 0;
						while (i.available() != 0 && bytes < IO_BUFFER_SIZE) {
							bytes = (short) (i.read(buffer, bytes, IO_BUFFER_SIZE - bytes) + bytes);
						}
						time = System.currentTimeMillis();
						o.write(TEMPOCommands.SEND2);
					
					}
					
					
					
					rawPacketBuffer.put(new TEMPOPacket(ByteBuffer.allocate(bytes).put(buffer, 0, bytes), time));

				} catch (IOException e) {
					// TODO Auto-generated catch block

					Arrays.fill(buffer, (byte) 0);
					bytes = (short) (this.BYTES_EXPECTED_PER_POLL);
					time = System.currentTimeMillis();
					rawPacketBuffer.put(new TEMPOPacket(ByteBuffer.allocate(bytes).put(buffer, 0, bytes), time));
					
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			cancel();

		}
			
		/**************************************************************************
		 * Stops collecting data
		 *****************************************************************************/
		public void cancel() {
			try {
				inSession = false;
				rawPacketBuffer = new TEMPOPacketBuffer(PACKET_BUFFER_SIZE);

				socket.getOutputStream().write(TEMPOCommands.STOP);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/**************************************************************************
		 * Sends the first poll and records the time of the poll
		 * @return If polling is successful returns the time the poll was sent, else -1
		 *****************************************************************************/
		private long initSession() {

			
			long time = 0;
			int bytes = 0;
			byte[] buffer = new byte[IO_BUFFER_SIZE];
			
			InputStream i = null;
			OutputStream o = null;

			try {
				i = socket.getInputStream();
				o = socket.getOutputStream();
				getCalibDataFromNode();
				o.write(TEMPOCommands.STOP);
				o.write(TEMPOCommands.SPS128);
				o.write(TEMPOCommands.START);
				do {
					o.write(TEMPOCommands.SEND2);
					Thread.sleep((long) SECONDS_PER_POLL * 1000);

					bytes = 0;

					while (i.available() != 0 && bytes < IO_BUFFER_SIZE) {
						bytes = (i.read(buffer, bytes, IO_BUFFER_SIZE - bytes) + bytes);
					}
					time = System.currentTimeMillis();
				} while (bytes < 6 || buffer[bytes - 2] != '\r'
						|| buffer[bytes - 1] != '\n');


			} catch (IOException e2) {
				// TODO Auto-generated catch block

				e2.printStackTrace();
				return -1;

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;

			}

			return time;
		}

		/**************************************************************************
		 * Gets calibration data from sensor node and writes it to a temporary file
		 *****************************************************************************/
		private void getCalibDataFromNode() {

			int bytes = 0;

			OutputStream o = null;
			byte[] buffer = new byte[IO_BUFFER_SIZE];
			
			try {
				o = socket.getOutputStream();
				o.write(TEMPOCommands.CSEND);
			} catch (IOException e3) {
			}



			try {
				InputStream i = socket.getInputStream();
				while(i.available() <= 0){
					Thread.sleep(2000);
			
				}
				while (i.available() != 0) {
					bytes = (i.read(buffer, bytes, IO_BUFFER_SIZE - bytes) + bytes);
				}
				calib = ByteBuffer.allocate(bytes).put(buffer, 0, bytes);
				o.write(TEMPOCommands.STOP);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		private ByteBuffer cloneByteBuffer(ByteBuffer original) {
			ByteBuffer clone = ByteBuffer.allocate(original.capacity());
			original.rewind();
			clone.put(original);
			original.rewind();
			clone.flip();
			return clone;
			
		}
		
		
		private class TEMPOPacketBuffer{
			
			private TEMPOPacket[] packets;
			private int currentPosition;
			private int size;
			
			public TEMPOPacketBuffer(int size){
				this.size = size;
				this.packets = new TEMPOPacket[size];
				this.currentPosition = 0;
			}
			
			public int getSize(){
				return size;
			}
			
			public TEMPOPacket[] getLastKPackets(int k) throws IndexOutOfBoundsException {
				if(k > size || k < 1) {
					throw new IndexOutOfBoundsException();
				} else {
					return accessPackets(false, null, k);
				}
			}
			
			public void put(TEMPOPacket packet){
				accessPackets(true, packet, 0);
			}
			
			public long getTimeLastPacketRecieved(){
				return getLastKPackets(1)[0].getTimeRecieved();
			}
			
			private synchronized TEMPOPacket[] accessPackets(boolean put, TEMPOPacket putData, int k) {
				TEMPOPacket[] ret = null;
				if(put) {
					packets[currentPosition] = new TEMPOPacket(putData); 
					currentPosition = (currentPosition + 1)%size;
				} else {
					ret = new TEMPOPacket[k];
					for(int i = k; i > 0; i--){
						ret[i-1] = new TEMPOPacket(packets[(currentPosition - i)%size]);	
					}
				}
				
				return ret;
				
			}
			
		}

	}

	
}
