package androidTEMPOAPI;



/**
 * This class contains the commands that may be sent to a TEMPO node.
 * 
 * @author Mohammed Nauage
 */

public class TEMPOCommands {

	public final static byte SEND = '^'; // Bluetooth Command to Send Data
	public final static byte SEND2 = '<'; // Bluetooth Command to Send Data with
											// SysCounter at the End
	public final static byte START = '>'; // Bluetooth Command to Start
											// Collection
	public final static byte STOP = '!'; // Bluetooth Command to Stop Collection
	public final static byte LPM = '~'; // Bluetooth Command to Stop Collection
	public final static byte SPS120 = '7'; // Bluetooth Command to Set 120 Hz
											// TB0 SPS
	public final static byte SPS60 = '6'; // Bluetooth Command to Set 60 Hz TB0
											// SPS
	public final static byte SPS128 = '5'; // Bluetooth Command to Set 128 Hz
											// TB0 SPS
	public final static byte SPS64 = '4'; // Bluetooth Command to Set 64 Hz TB0
											// SPS
	public final static byte SPS32 = '3'; // Bluetooth Command to Set 32 Hz TB0
											// SPS
	public final static byte SPS16 = '2'; // Bluetooth Command to Set 16 Hz TB0
											// SPS
	public final static byte SPS8 = '1'; // Bluetooth Command to Set 8 Hz TB0
											// SPS
	public final static byte CALIB = 'c'; // Bluetooth Command to Start
											// Calibration
	public final static byte CSEND = 'S'; // Bluetooth Command to Send
											// calibration data
	public final static byte CLK = 'k'; // Bluetooth Command to Send the
										// SysCounter value
	public final static byte VER = 'v'; // Bluetooth Command to Send the
										// Firmware Version

}
