package src.AndroidTempo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

/**
 * This class is used to manage Bluetooth connections and collect data.
 * 
 * @author Mohammed Nauage
 */
public class ConnectionService extends Service {


	public final static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	
	/** Directory in which all the data concerning this Application will be held. 
	*/
	public static File rootDir = new File(
			Environment.getExternalStorageDirectory(), "Android Tempo");
	/** Directory where all the temporary files for the session will exist.
	*/
	public static File tempDir = new File(rootDir, "temp");

	private final IBinder mBinder = new LocalBinder();
	private TreeMap<String, TEMPODevice> deviceMap = new TreeMap<String, TEMPODevice>();
	private TreeMap<String, Collector> collection = new TreeMap<String, Collector>();
    private Handler mHandler;
    
    private boolean lock;


    /**************************************************************************
     * Default binder description for this service
     *****************************************************************************/
	public class LocalBinder extends Binder {
		public ConnectionService getService() {
			return ConnectionService.this;
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
		
		if(!tempDir.exists()){
			tempDir.mkdir();
		}
		Toast.makeText(this, "Service Bound", Toast.LENGTH_LONG).show();
		return mBinder;
	}

	
	// starts and brings the service to the foreground making it so that it does
	// not become shutdown if the device is low on memory
	/**************************************************************************
	 * Prepares the service.
	 * @param intent The Intent supplied to startService(Intent), as given.
	 * @param flags Additional data about this start request. Currently either 
	 * 				0, START_FLAG_REDELIVERY, or START_FLAG_RETRY.
	 * @parm startId A unique integer representing this specific request to start.
	 * @return The return value indicates what semantics the system should use 
	 * 			for the service's current started state.
	 *****************************************************************************/
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		
		//Causes the service to run in the foreground decreasing its porbability of being
		//stopped due to low resources
		Notification notification = new Notification();
		Intent notificationIntent = new Intent(this, ConnectionService.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this,
				"Android Tempo Connection Service", "", pendingIntent);
		startForeground(1234, notification);

		return START_NOT_STICKY;
	}
	
	/**************************************************************************
	 * Setter for a mHandler. mHandler allows messages to be passed among the 
	 * service and other threads which have access to the handler
	 * @param handler a message passing interface
	 *****************************************************************************/
	public void setHandler(Handler handler){
		mHandler = handler;
	}
	//TODO:???
	// Gets and returns a pipe with data being streamed to it from the
	// node with mac address given. This pipe is used to stream data
	// to other activities.
	public PipedInputStream startPipeData(String mac) {

		return collection.get(mac).startPipeData();

	}

	// Closes the pipe to which data from the node with the given mac
	// address is being streamed. This pipe is used to stream data
	// to other activities.
	public void stopPipeData(String mac) {
		collection.get(mac).stopPipeData();
	}

	
	
	/**************************************************************************
	 * Creates and adds TEMPODevice object to a hashmap which is later used
	 * to identify which devices should be in the session.
	 * @param mac TEMPO node MAC
	 * @param name TEMPO node name
	 * @return TEMPODevice object created using the device MAC and name.
	 *****************************************************************************/
	public TEMPODevice addDeviceToSessionList(String mac, String name) {
		
		TEMPODevice temp = deviceMap.get(mac);
		if (temp == null) {
			try {
				temp = new TEMPODevice(BluetoothAdapter.getDefaultAdapter()
						.getRemoteDevice(mac), name);
				if (temp != null)
					deviceMap.put(mac, temp);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}

		}
		return temp;
	}

	/**************************************************************************
	 * Attempts to establish Bluetooth connection with the given TEMPO node
	 * @param node A TEMPODevice object which contains information concerning
	 * 				the node to which you want to connect.
	 * @return True if connection was established and false if it failed
	 *****************************************************************************/
	public boolean connectToDevice(TEMPODevice node) {
		if (node != null)
			return node.connect();
		return false;
	}
 
	/**************************************************************************
	 * Attempts to disconnect from the given device
	 * @param node A TEMPODevice object which contains information concerning
	 * 				the node from which you want to disconnect.
	 * @return True if disconnection was successful else false
	 *****************************************************************************/
	public boolean disconnectFromDevice(TEMPODevice inputDevice) {
		return inputDevice.disconnect();
	}

	/**************************************************************************
	 * Prepares all nodes set for the current session to be started by
	 * connecting to the nodes, creating the object which will handle data
	 * collection, and recording some initial data from the nodes.
	 * @return True if all nodes were successfully prepared else false.
	 *****************************************************************************/
	public boolean prepareAllNodesForSession() {
		Iterator<TEMPODevice> it = (Iterator<TEMPODevice>)deviceMap.values().iterator();
		TEMPODevice i = null;
		boolean ret = true;
		while(it.hasNext()){
			i = it.next();
		
			if(connectToDevice(i)){
				Collector collect = new Collector(i);
				i.setIsInSession(true);
				collection.put(i.getMac(), collect);
				collect.recordCalibData();
				
			} else {
				ret = false;
			}
		}
		return ret;
	}
	
	
	/**************************************************************************
	 * Prepares the node that is identified by the given MAC that exists in the
	 *  to be started by
	 * establishing a connection to the nodes, creating the object which will 
	 * handle data collection, and recording some initial data from the nodes.
	 * @return True if all nodes were successfully prepared else false.
	 *****************************************************************************/
	public boolean prepareNodeForSession(String mac) {


		TEMPODevice tmp = deviceMap.get(mac);
		boolean ret = true;
		
		if(tmp != null && connectToDevice(tmp)){
			if (!collection.containsKey(mac)) {
				Collector collect = new Collector(deviceMap.get(mac));
				
				deviceMap.get(mac).setIsInSession(true);
				
				collection.put(mac, collect);
				collect.recordCalibData();

			} else {
				ret = false;
			}

		}
		return ret;
	}

	
	/**************************************************************************
	 * Starts threads to begin collecting from all nodes
	 *****************************************************************************/
	public void startSession() {
		Iterator<Collector> it = (Iterator<Collector>)collection.values().iterator();
		Collector i = null;
		while(it.hasNext()){
			i = it.next();
			if(i != null){
				i.start();
			}
		}
		

	}

	/**************************************************************************
	 * Ends the session by disconnecting all nodes and clears all fields
	 * @return True it successfully disconnected from all nodes and cleared 
	 * 			all fields else false
	 *****************************************************************************/
	public boolean stopSession() {
		try {
			Iterator<Collector> collIt = collection.values().iterator();
			Iterator<TEMPODevice> devIt = deviceMap.values().iterator();
			Collector coll = null;
			TEMPODevice dev = null;
			while(collIt.hasNext()){
				coll = collIt.next();
				if(coll != null){
					coll.cancel();
					coll.join();
				}
			}
			while(devIt.hasNext()){
				dev = devIt.next();
				disconnectFromDevice(dev);
			}
			collection.clear();
			deviceMap.clear();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	
	/**************************************************************************
	 * Checks the collection state of all nodes
	 * @return True if collecting from all nodes else false
	 *****************************************************************************/
	public boolean allNodesCollecting() {
		boolean allNodesCollecting = true;
		for (Collector i : collection.values()) {
			allNodesCollecting &= i.isCollecting();
		}

		return allNodesCollecting;
	}

	private class Collector extends Thread {

		public final static float SECONDS_PER_POLL = 1f;
		
		public final static short BYTES_PER_SAMPLE = 12;
		
		public final static short SAMPLES_PER_SECOND = 128;

		public final static short BUFFER_SIZE = 4096;

		public final static byte MOD_LF = '\n' + 0x10;
		public final static byte MOD_CR = '\r' + 0x10;
		public final static int MAX_FILE_SIZE = (int) (BYTES_PER_SAMPLE * 1000 * SECONDS_PER_POLL * SAMPLES_PER_SECOND);

		private byte balance;
		private long curSysCounter;

		private boolean streamOut;
		private boolean isCollecting;

		private TEMPODevice device;

		private PipedOutputStream pipeOut;
		private byte[] buffer;

		private int currentFileNumber;
		private File tempDataFile;
		private FileOutputStream tempDataFileWriter;

		private String filePrefix;

		/**************************************************************************
		 * Class constructor
		 * @param device The device from which the object should collect data.
		 *****************************************************************************/
		public Collector(TEMPODevice device) {

			filePrefix = device.getName();
			currentFileNumber = 0;

			makeNewTempFile();

			isCollecting = false;

			this.device = device;
			balance = 0;
			curSysCounter = 0;
			
			
			streamOut = false;

			buffer = new byte[BUFFER_SIZE];

			try {
				tempDataFileWriter = new FileOutputStream(tempDataFile, true);

			} catch (FileNotFoundException e1) {

			}

			pipeOut = new PipedOutputStream();

		}

		/**************************************************************************
		 * Returns the collection state
		 * @return True if it is collecting data, else false
		 *****************************************************************************/
		public boolean isCollecting() {
			return isCollecting;
		}

		/**************************************************************************
		 * Gets calibration data from sensor node and writes it to a temporary file
		 *****************************************************************************/
		public void recordCalibData() {

			int bytes = 0;
			File calibFile = new File(tempDir, filePrefix
					+ "_calib.dat");
			FileOutputStream calibFS = null;

			OutputStream o = null;
			try {
				o = device.getSocket().getOutputStream();
				o.write(TEMPOCommands.CSEND);
			} catch (IOException e3) {
			}

			try {
				calibFS = new FileOutputStream(calibFile);
				Thread.sleep(2000);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				InputStream i = device.getSocket().getInputStream();

				while (i.available() != 0) {
					bytes = (i.read(buffer, bytes, BUFFER_SIZE - bytes) + bytes);
					calibFS.write(buffer, 0, bytes);
					bytes = 0;
				}

				o.write(TEMPOCommands.STOP);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


		/**************************************************************************
		 * Used to write the time the first accepted data value was recorded to a
		 * temporary file for syncing in the XML.
		 * @param time time in milliseconds
		 *****************************************************************************/
		private void recordStartTime(long time) {
			try {
				File f = new File(tempDir, filePrefix
						+ "_startTime.dat");
				if (!f.exists()) {
					DataOutputStream startTimeStream = new DataOutputStream(
							new FileOutputStream(f));
					startTimeStream.writeUTF(device.getName() + "\n");
					startTimeStream.writeLong(time);
					startTimeStream.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		
		//TODO???
		// Creates pipes to stream data from this class to other classes
		// and sets a boolean to start writing to that stream
		public synchronized PipedInputStream startPipeData() {

			PipedInputStream pipeIn = null;
			try {
				pipeIn = new PipedInputStream(pipeOut);
				streamOut = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return pipeIn;
		}

		// Sets a boolean to stop writing to an pipeOut
		public synchronized void stopPipeData() {
			streamOut = false;
		}

		/**************************************************************************
		 * Main loop for polling the node and collecting data
		 *****************************************************************************/
		public void run() {

			// initializes variables
			long time = 0;
			int bytes = 0;
			long numSent = 0;
			long numRecieved = 0;
			InputStream i = null;
			OutputStream o = null;

			try {
				i = device.getSocket().getInputStream();
				o = device.getSocket().getOutputStream();
				// gets the clock and time in milliseconds
				time = initCollection();

				o.write(TEMPOCommands.SEND2);

			} catch (IOException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}


			// main loop
			while (device.getIsInSession()) {
				
				if (tempDataFile.length() > MAX_FILE_SIZE) {
					makeNewTempFile();
				}
				try {

					if (System.currentTimeMillis() - time < (long) SECONDS_PER_POLL * 1000) {
						Thread.sleep((long) (SECONDS_PER_POLL * 1000)
								- (System.currentTimeMillis() - time));
					}

					if (device.getIsConnected()) {
						bytes = 0;
						while (i.available() != 0 && bytes < BUFFER_SIZE) {
							bytes = (i.read(buffer, bytes, BUFFER_SIZE - bytes) + bytes);
						}
						o.write(TEMPOCommands.SEND2);
						
						numSent++;
						
						bytes = (int) validation(bytes, curSysCounter);
						
						numRecieved = (curSysCounter == 0 ? numRecieved : numRecieved+1);
	            		
						String message = device.getMac() + "," + numRecieved + "/" + numSent;
	            		if(mHandler != null)
	            			mHandler.obtainMessage(0, message.length(), -1, message).sendToTarget();
					
					}
					time = System.currentTimeMillis();

					writeTempData(0, bytes);

					if (streamOut) {
						try {
							pipeOut.write(buffer, 0, bytes);
							pipeOut.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					device.disconnect();

					Arrays.fill(buffer, (byte) 0);
					bytes = (int) ((BYTES_PER_SAMPLE * SAMPLES_PER_SECOND 
							* System.currentTimeMillis() - time) / 1000);
					time = System.currentTimeMillis();
					
					writeTempData(0, bytes);
					isCollecting = false;

					e.printStackTrace();
				} catch (InterruptedException e2) {
					// TODO Auto-generated catch block

				}
			}

			cancel();

		}

		/**************************************************************************
		 * Makes a new temporary file to hold sensor data with properly formated name
		 *****************************************************************************/
		private void makeNewTempFile() {
				currentFileNumber++;
				tempDataFile = new File(tempDir, filePrefix + "_" + currentFileNumber + ".dat");
				try {
					tempDataFileWriter.close();
					tempDataFileWriter = new FileOutputStream(tempDataFile, true);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			
		}

		/**************************************************************************
		 * Sends the first poll and records the time of the poll
		 * @return If polling is successful returns the time the poll was sent, else -1
		 *****************************************************************************/
		private long initCollection() {

			long time = 0;
			int bytes = 0;

			InputStream i = null;
			OutputStream o = null;

			try {
				i = device.getSocket().getInputStream();
				o = device.getSocket().getOutputStream();
				o.write(TEMPOCommands.STOP);
				o.write(TEMPOCommands.SPS128);
				o.write(TEMPOCommands.START);
				do {

					o.write(TEMPOCommands.SEND2);
					Thread.sleep((long) SECONDS_PER_POLL * 1000);
					time = System.currentTimeMillis();

					bytes = 0;

					while (i.available() != 0 && bytes < BUFFER_SIZE) {
						bytes = (i.read(buffer, bytes, BUFFER_SIZE - bytes) + bytes);
					}

				} while (bytes < 6 || buffer[bytes - 2] != '\r'
						|| buffer[bytes - 1] != '\n');

				recordStartTime(time - (long) SECONDS_PER_POLL * 1000);
				isCollecting = true;

				curSysCounter = 16777216 * (0xff & (short) buffer[bytes - 6]) + 65536
						* (0xff & (short) buffer[bytes - 5]) + 256
						* (0xff & (short) buffer[bytes - 4])
						+ (0xff & (short) buffer[bytes - 3]);

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
		 * Validates data received by checking values received are acceptable and
		 * the number of bytes received are within an acceptable range, and appends
		 * 0s or drops bytes as needed.
		 * @param end Position of the last byte received in the buffer
		 * @param prevSysCounter the sysCounter from the previous packet received
		 * @return The number of acceptable bytes in the modified buffer
		 *****************************************************************************/
		private short validation(int end, long prevSysCounter) {

			byte[] buff = buffer;
			int diff;

			byte mcr = MOD_CR;
			byte mlf = MOD_LF;
			//Checks to see if it got the entire packet
			//and if it got the right number of bytes
			if (end <= 6 || buff[(end - 2)] != '\r' || buff[(end - 1)] != '\n'
					|| (end - 6) % BYTES_PER_SAMPLE != 0) {
				//sets curSysCounter as a flag
				curSysCounter = 0;
				//and calculates the expected number of data points that need to be recorded
				diff = (int) (BYTES_PER_SAMPLE * SECONDS_PER_POLL * SAMPLES_PER_SECOND);
				//sets all data points to 0
				Arrays.fill(buff, (byte) 0);


			} else if (curSysCounter == 0) {
				//gets new curSysCounter by which data validation can be done
				//and resets everything else
				curSysCounter = 16777216 * (0xff & (short) buff[(end - 6)]) + 65536
						* (0xff & (short) buff[(end - 5)]) + 256
						* (0xff & (short) buff[(end - 4)])
						+ (0xff & (short) buff[(end - 3)]);
				
				balance = 0;
				diff = (int) (BYTES_PER_SAMPLE * SECONDS_PER_POLL * SAMPLES_PER_SECOND);
				Arrays.fill(buff, (byte) 0);



			} else {
				//makes sure that if 0x10 was added by the to prevent 
				//a byte from being a \r or \n it is set to the correct value
				for (int i = 0; i < end - 6; i += 2) {
					if (buff[i] == mlf || buff[i] == mcr) {
						buff[i] -= 0x10;
					}
					if (buff[i] >= 0x10) {
						Arrays.fill(buff, (byte) 0);
						curSysCounter = 0;
						return (int) (BYTES_PER_SAMPLE * SECONDS_PER_POLL * SAMPLES_PER_SECOND);
					}
				}
				//convert 4 bytes to ints
				curSysCounter = 16777216 * (0xff & (short) buff[(end - 6)]) + 65536
						* (0xff & (short) buff[(end - 5)]) + 256
						* (0xff & (short) buff[(end - 4)])
						+ (0xff & (short) buff[(end - 3)]);
				
				//gets the difference in the expected number of 
				//data points recieved and the actual number
				diff = (int) (6 * (curSysCounter - prevSysCounter) - (end - 6));
				//adds diff to a varialbe which keeps track of 
				//the number of bytes the current data set is off by
				balance += diff;
				if (balance <= BYTES_PER_SAMPLE && balance >= -BYTES_PER_SAMPLE) {
					//if the number of bytes the current data set is 
					//off by is within acceptable ranges
					//it sets a variable to accept all that data from the packet
					diff = end - 6;


				} else if (balance > BYTES_PER_SAMPLE) {

					//If the data set has fewer points than allowable,
					//it pads the current data packet with 0's
					diff = (int) (BYTES_PER_SAMPLE * Math.floor(balance / BYTES_PER_SAMPLE));
					balance -= diff;
					diff += end - 6;

					for (int i = end - 6; i < diff; i++) {
						buff[i] = 0;
					}
				} else if (balance < -BYTES_PER_SAMPLE) {
					//If the data set has more points than allowable,
					//it only accepts a portion of the data.

					diff = (int) (BYTES_PER_SAMPLE * Math.ceil(balance / BYTES_PER_SAMPLE));
					balance -= diff;

					diff += end - 6;

				}
			}

			return (short) diff;

		}

		/**************************************************************************
		 * Writes a given number of bytes from a given offset from the buffer
		 * containing data 
		 * @param offset The position in the buffer from which data should start
		 * 					being written.
		 * @param count Number of bytes to be written.
		 *****************************************************************************/
		private void writeTempData(int offset, int count) {
			try {
				if (tempDir.canWrite()) {
					tempDataFileWriter.write(buffer, offset, count);

				}
			} catch (Exception e) {
			}
		}

		/**************************************************************************
		 * Stops collecting data
		 *****************************************************************************/
		public void cancel() {
			try {
				isCollecting = false;
				tempDataFileWriter.flush();
				tempDataFileWriter.close();
				device.getSocket().getOutputStream().write(TEMPOCommands.STOP);
				device.setIsInSession(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
