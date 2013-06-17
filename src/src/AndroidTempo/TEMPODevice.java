package src.AndroidTempo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;



/**
 * This class keeps track of device information.
 * @author Mohammed Nauage
 */

public class TEMPODevice extends Thread{

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
	private String location;
	private String name;
	private String MAC;
	private TEMPOPacketBuffer packetBuffer;
	private ByteBuffer calib;

	
	

	/**************************************************************************
	 * Class constructor
	 *****************************************************************************/
	public TEMPODevice(String mac) {
		device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac.toUpperCase());
		connected = false;
		inSession = false;
		socket = null;
		packetBuffer = new TEMPOPacketBuffer(PACKET_BUFFER_SIZE);
 
	}
	
	public TEMPODevice(String mac, String n) {
		device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac.toUpperCase());
		connected = false;
		inSession = false;
		socket = null;
		name = n;
		packetBuffer = new TEMPOPacketBuffer(PACKET_BUFFER_SIZE);

 
	}
	

	/**************************************************************************
	 * MAC getter
	 * @return the MAC value
	 *****************************************************************************/
	public String getMac() {
		if(device != null)
			return device.getAddress();
		else
			return null;
	}

	/**************************************************************************
	 * name getter
	 * @return the name value
	 *****************************************************************************/
	public String getNodeName() {
		if(name == null && device != null)
			return device.getName();
		else 
			return name;

	}

	public void setNodeName(String n){
		name = n;
	}
	
	public String getLocation() {
		// TODO Auto-generated method stub
		return location;
	}

	public void setLocation(String loc) {
		// TODO Auto-generated method stub
		location = loc;
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
	 * device getter
	 * @return the value of device
	 *****************************************************************************/
	public BluetoothDevice getDevice() {
		return device;
	}

	/**************************************************************************
	 * socket getter
	 * @return the value of socket
	 *****************************************************************************/
	public BluetoothSocket getSocket() {
		return socket;
	}

	/**************************************************************************
	 * Establishes a connection with the device
	 * @return If the connection was established properly, it returns true.
	 * 			Otherwise, it returns false.
	 *****************************************************************************/
	public boolean connect() {


	
			// 0xe is the version code for OS 4.0/API 14
			// Checks SDK and gets a socket using different methods based on the
			// socket
//			if (android.os.Build.VERSION.SDK_INT >= 0xe) {
//				socket = (BluetoothSocket) device.getClass()
//						.getMethod("createInsecureRfcommSocket", int.class)
//						.invoke(device, 1);
//			} else {
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
	
	public String toString(){
		return name
				+ "\n"
				+ MAC
				+ "\n"
				+ (location == null ? "" : location);
	}


	public int getMaxPacketsStored(){
		return packetBuffer.getSize();
	}

	public TEMPOPacket[] getLastKPackets(int k){
		return packetBuffer.getLastKPackets(k);		
	}
	
	public TEMPOPacket[] getAllPackets(){
		return packetBuffer.getLastKPackets(packetBuffer.getSize());
	}
	
	public TEMPOPacket[] getNewPackets(TEMPOPacket lastPacketRead) {
		TEMPOPacket[] packetsAvailable = packetBuffer.getLastKPackets(PACKET_BUFFER_SIZE);
		ArrayList<TEMPOPacket> ret = new ArrayList<TEMPOPacket>();
		if(lastPacketRead != null){
			for(TEMPOPacket i: packetsAvailable){
				if(i != null && i.getTimeRecieved() > lastPacketRead.getTimeRecieved()){
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
	
	public long getTimeLastPacketRecieved(){
		
		return packetBuffer.getTimeLastPacketRecieved();
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
			i = this.getSocket().getInputStream();
			o = this.getSocket().getOutputStream();
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
				
				
				
				packetBuffer.put(new TEMPOPacket(ByteBuffer.allocate(bytes).put(buffer, 0, bytes), time));

			} catch (IOException e) {
				// TODO Auto-generated catch block

				Arrays.fill(buffer, (byte) 0);
				bytes = (short) (this.BYTES_EXPECTED_PER_POLL);
				time = System.currentTimeMillis();
				packetBuffer.put(new TEMPOPacket(ByteBuffer.allocate(bytes).put(buffer, 0, bytes), time));
				
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
			packetBuffer = new TEMPOPacketBuffer(PACKET_BUFFER_SIZE);

			this.getSocket().getOutputStream().write(TEMPOCommands.STOP);
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
			i = this.getSocket().getInputStream();
			o = this.getSocket().getOutputStream();
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
			o = this.getSocket().getOutputStream();
			o.write(TEMPOCommands.CSEND);
		} catch (IOException e3) {
		}



		try {
			InputStream i = this.getSocket().getInputStream();
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
				for(int i = 1; i <= k; i++){
					ret[i] = new TEMPOPacket(packets[(currentPosition - i)%size]);	
				}
			}
			
			return ret;
			
		}
		
	}

}
