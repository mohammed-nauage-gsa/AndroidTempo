package src.AndroidTempo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import android.app.Activity;

/**
 * This class is used to generate an XML from dat files.
 * 
 * @author Mohammed Nauage
 */
public class XMLGenerator extends Activity {

	public static final String template = "BlueTempoXMLTemplate.xml";

	private long syncTime;
	private Vector<Long> startTimes = new Vector<Long>(7);
	private Vector<String> filePrefixToNodeName = new Vector<String>(7);
	private File directory;
	private FileWriter xmlStream;
	private BufferedReader templateReader;
	private File xml;

	/**************************************************************************
	 * Class Constructor
	 *****************************************************************************/
	public XMLGenerator(File dir, InputStream temp) {

		templateReader = new BufferedReader(new InputStreamReader(temp));
		directory = dir;
		//gets the start time
		//and all other times if needed
		getTimes();

		//creates properly formated file name
		//in AndroidTempo folder
		xml = new File(dir.getParent(), "AndroidTempoData-"
				+ new SimpleDateFormat("yyMMdd-HHmm")
						.format(new Date(syncTime)).toString() + ".xml");
		try {
			xmlStream = new FileWriter(xml);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**************************************************************************
	 * Generates XML
	 *****************************************************************************/
	public void generateXML() {
		String n, f;
		n = null;
		f = null;
		try {
			while (templateReader.ready()) {
				f = templateReader.readLine();

				xmlStream.write(f + "\n");
				// goes through the template and does different things based on
				// the tag it finds
				if (f.equals("<Name> ") && n == null) {
					n = templateReader.readLine();
					xmlStream.write(n + "\n");

				} else if (f.equals("<Data>") && n != null) {
					if (n.equals("Sensor IDs")) {
						//writes node names with commas separating them
						for (int i = 0; i < filePrefixToNodeName.size() - 1; i++) {
							xmlStream.write(filePrefixToNodeName.get(i) + ",");

						}
						//adds the last element
						xmlStream.write(filePrefixToNodeName.lastElement());

					} else if (n.equals("Annotations")) {
						writeAnnotations();
					} else if (n.equals("Timestamps")) {
						writeTimestamps();
					} else if (n.equals("Date")) {
						//converts the milliseconds to date and writes it to a file
						xmlStream.write(new SimpleDateFormat(
								"MM/dd/yyyy,hh:mm:ss a").format(
								new Date(syncTime)).toString());
					} else if (n.equals("Sampling Rate (Hz)")) {
						//does nothing as it seems that this is predefined
					} else if (n.equals("Sensor Locations")) {
						this.writeLocations();
					} else if (n.contains("Sensor ")) {
						// fill in???
						
						//writes sensor data
						writeData(n.charAt(n.length() - 1) - '1');

					} else if (n.equals("# of active sensors")) {
						//writes number of sensors in session
						xmlStream.write(startTimes.size() + "");
					} else if (n.contains("Conversion")) {
						//writes calibration data
						writeConversions(n.charAt(n.length() - 1) - '1');
					} else if (n.contains("Description")) {
						
						//writes the session description
						File in = new File(directory, "Description.dat");
						BufferedReader descReader = new BufferedReader(
								new InputStreamReader(new FileInputStream(in)));
						StringBuffer description = new StringBuffer();
						while (descReader.ready()) {
							description.append(descReader.readLine() + "\n");
						}
						xmlStream.write(description.substring(0,
								description.length() - 1));
					}
					n = null;

				}

			}
			xmlStream.flush();
			xmlStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void getTimes() {
		int j = 0;
		DataInputStream stream = null;
		File file = null;

		//goes through all the start time files
		//and decides which one is the lates and sets that as the time when all of them started
		while ((file = new File(directory, j + "_startTime.dat")).exists()) {
			try {
				stream = new DataInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				String name = "";
				char tmp;
				while ((tmp = (char) stream.readByte()) != '\n') {
					name += tmp;
				}
				//maps file prefix to node names
				//associates name to the sensor number
				filePrefixToNodeName.add(name.trim());
				startTimes.add(stream.readLong());
				syncTime = syncTime > startTimes.get(j) ? syncTime : startTimes
						.get(j);
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			j++;

		}
	}

	private void writeConversions(int i) {
		// TODO Auto-generated method stub
		File in = null;
		DataInputStream ins = null;

		if ((in = new File(directory, i + "_calib" + ".dat")).exists()) {
			try {
				ins = new DataInputStream(new FileInputStream(in));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {

				ins.skipBytes(14);
				
				xmlStream.write(ins.readUnsignedByte() + 256
						* ins.readUnsignedByte() + ",");
				//reads bytes and and converts them to integers
				xmlStream.write((ins.readUnsignedByte()) + 256
						* (ins.readUnsignedByte()) + 65536
						* (ins.readUnsignedByte()) + 16777216
						* (ins.readUnsignedByte()) + ",");

				StringBuffer str = new StringBuffer();
				
				while (ins.available() != 0)
					str.append((short) (ins.readUnsignedByte() + 256 * ins
							.readUnsignedByte()) + ",");

				xmlStream.write(str.substring(0, str.length() - 1));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private void writeTimestamps() {
		// TODO Auto-generated method stub
		long time = 0;
		File in = new File(directory, "Timestamps.dat");
		if (in.exists()) {
			DataInputStream ins = null;
			try {
				ins = new DataInputStream(new FileInputStream(in));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				
				while (ins.available() != 0) {
					time = ins.readLong();
					//writes the time since the beginning of session in seconds 
					//that have passed that corresponds to an annotation
					if (ins.available() != 0)
						xmlStream.write(((time - syncTime) / 1000.0d) + ",");

				}
				xmlStream.write(((time - syncTime) / 1000.0d) + "");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private void writeData(int i) {
		int j = 0;
		File in = null;
		DataInputStream ins = null;
		// byte[] buffer = new byte[12000];
		// int bytes = 0;
		long skip = 0;
		//goes through Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, and Zgyro data
		for (int k = 0; k < 12; k += 2) {
			//goes through every file of a single node
			j = 0;
			while ((in = new File(directory, i + "_" + j + ".dat")).exists()
					&& j < 1) {
				try {
					ins = new DataInputStream(new FileInputStream(in));

				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					//skips the proper amount to get the starting value of the correct data
					if (j == 0)
						ins.skip(skip);
					ins.skip(k);
					while (ins.available() > 0) {
						//formats it correctly and writes it to xml
						xmlStream.write((ins.readShort()) + ".000,");
						//skips to next value
						ins.skip(10);
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				j++;

			}

			//ends each data set with a 0.000
			try {
				if ((in = new File(directory, i + "_" + 0 + ".dat")).exists())
					xmlStream.write("0.000\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void writeAnnotations() {

		File in = new File(directory, "Annotations.dat");
		if (in.exists()) {
			BufferedReader ins = null;
			try {
				ins = new BufferedReader(new InputStreamReader(
						new FileInputStream(in)));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				//goes through the dat file and adds the strings to a buffer
				StringBuffer str = new StringBuffer();
				while (ins.ready()) {
					str.append(ins.readLine());

				}
				//writes the buffer to xml
				if (str.length() >= 2)
					xmlStream.write(str.substring(0, str.length() - 1));

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private void writeLocations() {
		BufferedReader stream = null;
		File file = new File(directory, "namesAndLocations.dat");

		if (file.exists()) {
			try {
				stream = new BufferedReader(new FileReader(file));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				String loc = "";
				String name = "";

				String[] locs = new String[7];
				StringBuffer out = new StringBuffer();
				while (stream.ready()) {
					//adds all the locations from the dat file to a String array
					//to make sure everything is written in the correct order
					name = stream.readLine();
					loc = stream.readLine();
					if (this.filePrefixToNodeName.contains(name))
						locs[this.filePrefixToNodeName.indexOf(name)] = loc;
				}
				//goes through the array and appends them to a StringBuffer
				for (String i : locs) {
					if (i != null)
						out.append(i + ",");
				}
				
				//writes the buffer to the xml
				if (out.length() > 1)
					xmlStream.write(out.substring(0, out.length() - 1));
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
