package src.AndroidTempo;

import java.io.IOException;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.media.Ringtone;
import android.media.RingtoneManager;


/**
 * This class keeps track of device information.
 * @author Mohammed Nauage
 */

public class TEMPODevice {

	private boolean isInSession;
	private BluetoothDevice device;
	private BluetoothSocket socket;
	private String name;
	private boolean isConnected;

	/**************************************************************************
	 * Class constructor
	 *****************************************************************************/
	public TEMPODevice(BluetoothDevice d, String n) {
		device = d;
		name = n;
		isConnected = false;

	}

	/**************************************************************************
	 * isInSession setter
	 * @param isInSession session state
	 *****************************************************************************/
	public void setIsInSession(boolean isInSession) {
		this.isInSession = isInSession;
	}

	/**************************************************************************
	 * name setter
	 * @param name device name
	 *****************************************************************************/
	public void setName(String name) {
		this.name = name;
	}

	/**************************************************************************
	 * MAC getter
	 * @return the MAC value
	 *****************************************************************************/
	public String getMac() {
		return device.getAddress();
	}

	/**************************************************************************
	 * name getter
	 * @return the name value
	 *****************************************************************************/
	public String getName() {
		return name;
	}

	/**************************************************************************
	 * isConnected getter
	 * @return the isConnected value
	 *****************************************************************************/
	public boolean getIsConnected() {
		if (socket == null)
			return false;

		return isConnected;
	}

	/**************************************************************************
	 * isInSession getter
	 * @return the isInSession value
	 *****************************************************************************/
	public boolean getIsInSession() {
		return isInSession;
	}

	/**************************************************************************
	 * Indicates whether some other device is "equal to" this one.
	 * @param device other device
	 * @return If the the devices are the same, it returns true. Otherwise,
	 * 			it returns false.
	 *****************************************************************************/
	public boolean equals(TEMPODevice device) {
		if (device.getSocket().equals(socket) && device.getDevice().equals(this.device)) {
			return true;
		}
		return false;
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

		// starts discovery
		if (!BluetoothAdapter.getDefaultAdapter().isDiscovering())
			BluetoothAdapter.getDefaultAdapter().startDiscovery();

		try {
			// 0xe is the version code for OS 4.0/API 14
			// Checks SDK and gets a socket using different methods based on the
			// socket
			if (android.os.Build.VERSION.SDK_INT >= 0xe) {
				socket = (BluetoothSocket) device.getClass()
						.getMethod("createInsecureRfcommSocket", int.class)
						.invoke(device, 1);
			} else {
				socket = device.createRfcommSocketToServiceRecord(UUID
						.fromString(ConnectionService.SPP_UUID));
			}
		} catch (Exception e) {

			socket = null;
		}

		if (socket != null) {
			// stops discovery
			BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
			try {
				// calls default BluetoothDevice function to connect and sets a
				// boolean
				// which states whether a device is connected or not
				socket.connect();
				isConnected = true;
			} catch (Exception e) {

				try {
					socket.close();

				} catch (Exception ee) {
					ee.printStackTrace();
				}
				e.printStackTrace();
			}

			return isConnected;
		}
		return false;
	}


	/**************************************************************************
	 * Closes Bluetooth connection
	 * @return If the connection was closed properly, it returns true. Otherwise,
	 * 			it returns false.
	 *****************************************************************************/
	public boolean disconnect() {
		try {
			socket.close();
			isConnected = false;
		} catch (IOException e) {

		}
		return !isConnected;
	}

}
