package src.AndroidTempo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

/**
 * This class is the activity that is used to discover and select TEMPO
 * nodes and input information concerning the session and the nodes that will be
 * used in the data collection.
 * 
 * @author Mohammed Nauage
 */
public class Main extends Activity {
	
	/** Request code that the function
	* onActivityResult() uses to identify that the result came
	* from an activity that enables Bluetooth.
	*/
	public static final int REQUEST_ENABLE_BT = 1;
	
	/** Request code that the function
	* onActivityResult() uses to identify that the result came
	* from the Monitor activity.
	*/
	public static final int REQUEST_MONITOR = 2;
	
	/** Directory in which all the data concerning this Application will be held. 
	*/
	public static File rootDir = new File(
			Environment.getExternalStorageDirectory(), "Android Tempo");
	/** Directory where all the temporary files for the session will exist.
	*/
	public static File tempDir = new File(rootDir, "temp");
	/** Directory where all the past configuration information is held.
	*/
	public static File configurationFolder = new File(rootDir, "Configurations");


	/**
	 * Contains data concerning the items in the list view
	 */
	private SelectionAdapter btArrayAdapter;

	/**
	 * Contains the string that the user specifies to describe the session.
	 */
	private String sessionDescription;

	/**
	 * Contains a mapping of node names to node MACs.
	 */
	private Hashtable<String, String> devices;

	/**
	 * Contains the locations of the TEMPO nodes specified by the user in the form
	 * of a string and maps it to the name of the node
	 */
	private Hashtable<String, String> locations;

	/**
	 * Contains the list of annotations expected to be used in the current session
	 */	
	private ArrayList<String> annotationList;

	/**
	 * Contains the node names that are a part of a predefined configuration that has been selected
	 */
	private String[] configNodes;

	


	/**************************************************************************
	 * Gets the node MACs and names from a file and stores it in a Hashtable.
	 *****************************************************************************/
	private void getNodeIds() {
		// Looks for the devices.csv file which contains the list of device
		// names and MACs
		File devicesCsv = new File(rootDir, "devices.csv");
		BufferedReader reader = null;
		

		// Once the file exists and is properly up to date, it fills a HashTable
		// with the names and MACs.
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(devicesCsv)));
		} catch (FileNotFoundException e) {
		}
		try {
			String s;
			while (reader.ready()) {
				s = reader.readLine();
				String[] sa = s.split(",");
				String mac = sa[0].toUpperCase();
				String name = sa[1].toUpperCase();
				devices.put(mac, name);
			}
		} catch (IOException e) {
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
		
		
		if (!configurationFolder.exists() || !configurationFolder.isDirectory()) {
			configurationFolder.mkdir();
		}

		// Checks if a temp folder exists and if not it makes it
		if (!tempDir.isDirectory())
			tempDir.mkdir();
		
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

	/**************************************************************************
	 * Initializes member fields
	 * @param savedInstanceState holds all the data that may be saved when
	 * 							onSaveInstanceState
	 *****************************************************************************/
	private void initVars(Bundle savedInstanceState){
		// Creates a hashtable that will map locations(strings) to device names
		locations = new Hashtable<String, String>();
		// which will store the description of the session.
		sessionDescription = "";

		annotationList = new ArrayList<String>();
		configNodes = new String[0];
		
		
		// initializes an empty ListView
		ListView devicesFound = (ListView) findViewById(R.id.deviceList);
		btArrayAdapter = new SelectionAdapter(
				devicesFound.getContext(), new ArrayList<String>());
		devicesFound.setAdapter(btArrayAdapter);
		devicesFound.setOnItemClickListener(new ItemClickListener());
		
		devices = new Hashtable<String, String>();
		getNodeIds();
		
		
	}
	
	/**************************************************************************
	 * Initializes Activity
	 * @param savedInstanceState holds all the data that may be saved when
	 * 							onSaveInstanceState
	 *****************************************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.config);

		
		createDefaultFiles();
		
		
		// registers a receiver which listens for the detection of bluetooth
		// devices, and acts appropiately for this application
		registerReceiver(ActionFoundReceiver, new IntentFilter(
				BluetoothDevice.ACTION_FOUND));




		initVars(savedInstanceState);
		// Checks the bluetooth state and prompts user appropriately to get it
		// in the correct state
		checkBlueToothState();

	}

	/**************************************************************************
	 * Deallocates resources for this activity
	 *****************************************************************************/
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			// unregister and stop services
			unregisterReceiver(ActionFoundReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	/**************************************************************************
	 * Listens for any clicks on ListView items and responds appropriately.
	 *****************************************************************************/
	private class ItemClickListener implements OnItemClickListener {

		/**************************************************************************
		 * Prompts the user to set a location where the user would
		 * like put the node on, and maps the location to the node name.
		 * @param adpt The AdapterView where the click happened.
		 * @param itemView The view within the AdapterView that was clicked (this will be a view provided by the adapter)
		 * @param pos The position of the view in the adapter.
		 * @param arg3 The row id of the item that was clicked.
		 *****************************************************************************/
		public void onItemClick(AdapterView<?> adpt, final View itemView, final int pos,
				long arg3) {
			// TODO Auto-generated method stub

			// gets the string that is shown on the ListView item that was
			// clicked
			final String item = (String) adpt.getItemAtPosition(pos);
			// Checks selection/setting location states
			// Creates an dialog box builder
			AlertDialog.Builder alert = new AlertDialog.Builder(
					adpt.getContext());
			
			// Creates a location an object where users may input text
			final EditText input = new EditText(Main.this);
			input.setOnKeyListener(new OnKeyListener(){

				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// TODO Auto-generated method stub
					if(keyCode == KeyEvent.KEYCODE_ENTER);
					return false;
				}
				
			});
			input.setText(locations.get(item.substring(0,
					item.indexOf("\n"))));
			// adds it to the builder
			alert.setView(input);
			// sets the default message of the alert box
			alert.setMessage("Enter Location for "
					+ item.substring(0, item.indexOf('\n')));
			
			// sets what happens when you press OK
			alert.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// gets the location text and replaces new
							// lines with spaces for the location text
							String loc = input.getText().toString()
									.replace("\n", " ");
							// gets the name of the node
							String[] itemParts = item.split("\n");
							String name = itemParts[0];
							

							// gets the position in the ListView the
							// user clicked
							int p = btArrayAdapter
									.getPosition(item);

							// replaces the value in the hashtable that
							// contains location information
							locations.put(name, loc);

							btArrayAdapter.updateConfigNodesNotFound(itemParts[0]
									+ "\n" + itemParts[1] + "\n" + loc);
							
							// replaces the olds string with a new
							// string that contains location information
							btArrayAdapter.remove(item);
							btArrayAdapter.insert(itemParts[0]
									+ "\n" + itemParts[1] + "\n" + loc, p);
							
							System.out.println(itemParts[1]);
							
							// updates ListView
							btArrayAdapter.notifyDataSetChanged();
							
						}
					});
			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			alert.create().show();

		}

	}

	/**************************************************************************
	 * Prompts the user for a description of the session.
	 * @param view The view that was clicked
	 *****************************************************************************/
	public void onSessionDescriptionClick(View view) {

		// creates alert box which takes in text input
		AlertDialog.Builder alert = new AlertDialog.Builder(
				Main.this);
		final EditText input = new EditText(Main.this);
		input.setOnKeyListener(new OnKeyListener(){

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode == KeyEvent.KEYCODE_ENTER);
				return false;
			}
			
		});
		input.setText(sessionDescription);
		alert.setView(input);

		alert.setMessage("Enter Session Description:");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
				//stores the description of the session in a variable
				sessionDescription = input.getText().toString();


			}
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		alert.create().show();

	}

	/**************************************************************************
	 * Writes the names and locations of the nodes and the session description 
	 * to a file.  Starts the Monitor activity and sends it information 
	 * concerning the nodes that were selected.
	 * @param view The view that was clicked
	 *****************************************************************************/
	public void onStartSessionClick(View view) {

		Vector<String> selected = btArrayAdapter.getSelected();

		if(selected.size() > 0){
			// sends Monitor the set of nodes that were selected for the session
			Intent i = new Intent(Main.this, SessionMonitor.class);
			i.putExtra("selected", selected);
			Collections.sort(annotationList);
			i.putExtra("annotations", annotationList);
	
			try {
				// writes names and locations of the selected nodes into
				// a file which will be later used by datToXML.java
				FileWriter nameAndLocWriter = new FileWriter(new File(tempDir,
						"namesAndLocations.dat"));
				String name;
				for (int j = 0; j < selected.size(); j++) {
					name = selected.get(j).substring(0,
							selected.get(j).indexOf('\n'));
					nameAndLocWriter.write(name + "\n"
							+ locations.get(name) + "\n");
	
				}
				nameAndLocWriter.flush();
				nameAndLocWriter.close();
				
					// writes to a file the description given
					// and saves it in a variable
	
					FileWriter descriptionFile = new FileWriter(new File(tempDir,
							"Description.dat"));
					descriptionFile.write(sessionDescription);
					descriptionFile.flush();
					descriptionFile.close();
	
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// starts Monitor activity
			startActivityForResult(i, REQUEST_MONITOR);
		} else {
		
			Context context = getApplicationContext();
			CharSequence text = "Please Select Nodes";
			int duration = Toast.LENGTH_LONG;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		}

	}
	/**************************************************************************
	 * Prompts the user to select a configuration file, and sets that as the
	 * current configuration.
	 * @param view The view that was clicked
	 *****************************************************************************/
	public void onLoadConfigurationClick(View view) {

		// creates alert box which takes in text input
		AlertDialog.Builder alert = new AlertDialog.Builder(
				Main.this);
		ArrayAdapter<String> adapter = 
				new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,this.configurationFolder.list());
		final Spinner input = new Spinner(Main.this);		
		input.setAdapter(adapter);
		
		input.setOnKeyListener(new OnKeyListener(){

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				
				if(keyCode == KeyEvent.KEYCODE_ENTER);
				return false;
			}
			
		});
		alert.setView(input);

		alert.setMessage("Enter Configuration File");

		alert.setPositiveButton("Load", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					String[] line;
					BufferedReader configFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(configurationFolder,
							input.getSelectedItem().toString()))));
					line = configFileReader.readLine().replace("\n", "").split(",");
					configNodes = line;
					line = configFileReader.readLine().replace("\n", "").split(",");
					Hashtable<String, String> currentLocations = locations;
					String[] tempConfigNodes = configNodes;
					
					for(int i = 0; i < tempConfigNodes.length; i++){
						currentLocations.put(devices.get(tempConfigNodes[i]), line[i]);
					}
					locations = currentLocations;
					
					line = configFileReader.readLine().replace("\n", "").split(",");
					annotationList = new ArrayList<String>();
					for(int i = 0; i < line.length; i++){
						annotationList.add(line[i]);
					}
					
					
					sessionDescription = configFileReader.readLine().replace("\n","");
					
					

					btArrayAdapter.clearSelected();
					btArrayAdapter.clear();
					Arrays.sort(configNodes);
					addConfigurationNodes();
					

					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				BluetoothAdapter.getDefaultAdapter().startDiscovery();


			}
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		alert.create().show();

	}
	
	/**************************************************************************
	 * Adds configuration nodes to the ListView
	 * @param view The view that was clicked
	 *****************************************************************************/
	private void addConfigurationNodes(){
		String[] tempConfigNodes = configNodes;
		btArrayAdapter.clearConfigNodesNotFound();
		for(int i = 0; i < tempConfigNodes.length; i++){
			String item = formatListItem(tempConfigNodes[i]);
			btArrayAdapter.add(item);
			btArrayAdapter.addToConfigNodesNotFound(item);
			btArrayAdapter.addSelected(item);

		}
		
	}
	
	/**************************************************************************
	 * Formats ListView Strings
	 * @param view The view that was clicked
	 *****************************************************************************/
	private String formatListItem(String MAC){
		String loc = (locations.get(devices.get(MAC)));
		String formattedItem = devices.get(MAC)
				+ "\n"
				+ MAC
				+ "\n"
				+ (loc == null ? "" : loc);
		return formattedItem;
	}
	
	/**************************************************************************
	 * Prompts the user for a file name. Saves current configuration 
	 * information in that file.
	 * @param view The view that was clicked
	 *****************************************************************************/
	public void onSaveConfigurationClick(View view) {

		// creates alert box which takes in text input
		AlertDialog.Builder alert = new AlertDialog.Builder(
				Main.this);
		final EditText input = new EditText(Main.this);
		alert.setView(input);
		input.setOnKeyListener(new OnKeyListener(){

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode == KeyEvent.KEYCODE_ENTER);
				return false;
			}
			
			
		});
		alert.setMessage("Enter Configuration Name");

		alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
				try {
					FileWriter configFileWriter = new FileWriter(new File(configurationFolder,
							input.getText().toString()));

					String saveData = "";
					Vector<String> selected = btArrayAdapter.getSelected();
					String[] selectedMACs;
					if(selected != null && selected.size() > 0){
						for(int i = 0; i < selected.size()-1; i++){
							saveData += selected.get(i).split("\n")[1] + ",";
						}
						saveData += selected.lastElement().split("\n")[1];
					}
					selectedMACs = saveData.replace("\n", "").split(",");
					saveData += "\n";
					
					Hashtable<String,String> tempLocations = locations;
					
					if(tempLocations != null && !tempLocations.isEmpty()){
						String location = "";
						for(int i = 0; i < selectedMACs.length-1; i++){
							location = tempLocations.get(devices.get(selectedMACs[i]));
							saveData += (location != null ? location : "") + ",";							
						}
						
						location = tempLocations.get(devices.get(selectedMACs[selectedMACs.length-1]));
						saveData += (location != null ? location : "");
					}
					
					saveData += "\n";
					
					if(annotationList != null){
						String[] tempAnnotationList = new String[annotationList.size()];
						annotationList.toArray(tempAnnotationList);
						String annotationListString = Arrays.toString(tempAnnotationList);
						saveData += annotationListString.substring(1, annotationListString.length()-1);						
					}
					 
					saveData += "\n" + sessionDescription;
					
					
					configFileWriter.write(saveData);
					configFileWriter.flush();
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}
		});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		
		alert.create().show();

	}
	
	/**************************************************************************
	 * Prompts user to create an annotation list, and stores the list to be
	 * used during the data collection session and as a part of a saved 
	 * configuration. 
	 * @param view The view that was clicked
	 *****************************************************************************/
	public void onMakeAnnotationListClick(View view) {

		// creates alert box which takes in text input
		AlertDialog.Builder alert = new AlertDialog.Builder(
				Main.this);
		final EditText input = new EditText(Main.this);
		input.setOnKeyListener(new OnKeyListener(){

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode == KeyEvent.KEYCODE_ENTER);
				return false;
			}
		});
		ArrayList<String> tempAnnotationList = annotationList;
		String currentListString = "";
		if(tempAnnotationList.size() > 0){
			for(int i = 0; i < tempAnnotationList.size()-1; i++){
				currentListString += tempAnnotationList.get(i) + ",";
			}
			currentListString += tempAnnotationList.get(tempAnnotationList.size()-1);
		}
		input.setText(currentListString);
		alert.setView(input);

		alert.setMessage("Enter Annotations(\",\" separated)");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
				String[] currentList = input.getText().toString().split(",");
				annotationList = new ArrayList<String>();
				for(int i = 0; i < currentList.length; i++){
					annotationList.add(currentList[i]);
				}

			}
		});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		
		alert.create().show();

	}
	
	/**************************************************************************
	 * Starts the process of searching for Bluetooth
	 * devices within range.
	 * @param view The view that was clicked
	 *****************************************************************************/
	public void onLookForNodesClick(View view) {

		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

		// clears everything that may be changed due to searching for
		// bluetooth devices
		btAdapter.cancelDiscovery();
		btArrayAdapter.clear();
		btArrayAdapter.clearSelected();
		addConfigurationNodes();
		// starts looking for devices
		btAdapter.startDiscovery();

	}

	/**************************************************************************
	 * Checks to see if Bluetooth is supported, and if
	 * Bluetooth is not enabled it enables it.
	 *****************************************************************************/
	private void checkBlueToothState() {
		TextView stateBluetooth = (TextView) findViewById(R.id.config);
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			// Tells the user bluetooth is not supported by the device
			stateBluetooth.setText(getString(R.string.btNotSupported));
		} else {
			if (btAdapter.isEnabled()) {
				// Sets a string to properly identify the ListView and its
				// meaning
				stateBluetooth.setText(getString(R.string.nodesFound));
				BluetoothAdapter.getDefaultAdapter().startDiscovery();
			} else {
				// Starts the bluetooth enabling process if user has bluetooth
				// but does not have it enabled
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	
	private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {

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

				// compares it to the MACs of tempo devices and makes sure it
				// does not already exist in the ListView
				if (devices.get(macAddress) != null) {
					String item = formatListItem(macAddress);
					
					
					if (btArrayAdapter.getPosition(item) == -1) {
						// if it is a tempo node and it does not exist in the
						// list view it is added to the ListView
						btArrayAdapter.add(item);
					}
					if(Arrays.binarySearch(configNodes, macAddress) >= 0){
						btArrayAdapter.removeFromConfigNodesNotFound(formatListItem(macAddress));
					}

					btArrayAdapter.notifyDataSetChanged();


				}
			}
		}
	};

	/**************************************************************************
	 * Called when an activity you launched exits, giving you the requestCode 
	 * you started it with, the resultCode it returned, and any additional data 
	 * from it. The resultCode will be RESULT_CANCELED if the activity explicitly 
	 * returned that, didn't return any result, or crashed during its operation.
	 * @param requestCode The integer request code originally supplied to 
	 * 						startActivityForResult(), allowing you to identify who 
	 * 						this result came from.
	 * @param resultCode The integer result code returned by the child activity 
	 * 						through its setResult().
	 * @param intent An Intent, which can return result data to the caller 
	 * 					(various data can be attached to Intent "extras").
	 *****************************************************************************/
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == REQUEST_MONITOR) {
			// If it comes back from the Monitor activity,
			// it resets all ProgramState
			btArrayAdapter.clear();
			btArrayAdapter.clearSelected();
			btArrayAdapter.notifyDataSetChanged();


			File[] files = tempDir.listFiles();

			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
			tempDir.delete();
			tempDir = new File(rootDir, "temp");
			tempDir.mkdir();

			checkBlueToothState();

			registerReceiver(ActionFoundReceiver, new IntentFilter(
					BluetoothDevice.ACTION_FOUND));

		} else if (requestCode == REQUEST_ENABLE_BT) {
			// If it comes back from the enabling bluetooth state activity
			// and bluetooth has been enabled, it changes state 
			// to enabled variable.
			BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
			if (btAdapter.isEnabled()) {
				btAdapter.cancelDiscovery();
				btAdapter.startDiscovery();
			}	
		}
	}

	/**************************************************************************
	 * This class keeps track of information concerning a ListView(what is
	 * or is not selected and what exists in a ListView).
	 *****************************************************************************/
	private class SelectionAdapter extends ArrayAdapter<String> {

		private final Context context;
		private ArrayList<String> vals, configNodesNotFound;
		private Vector<Integer> selected;

		/**************************************************************************
		 * Constructs an instance of SelectionAdapter that sets the values
		 * @param context The current context.
		 * @param pValues The list of strings that will initially be in the ListView.
		 *****************************************************************************/
		public SelectionAdapter(Context context, ArrayList<String> pValues) {
			super(context, R.layout.row, pValues);
			this.context = context;
			vals = pValues;
			selected = new Vector<Integer>();
			configNodesNotFound = new ArrayList<String>();
		}
		/**************************************************************************
		 * Removes all elements from the selected vector
		 *****************************************************************************/
		public void clearSelected() {
			// TODO Auto-generated method stub
			selected.clear();
			this.notifyDataSetChanged();
		}
		
		/**************************************************************************
		 * Adds elements to the selected vector
		 * @param node The string in the form of what is expected to be seen
		 * 						in the listView(name\nMAC\nlocation) which represents a node.
		 *****************************************************************************/
		public void addToConfigNodesNotFound(String node){
			if(!configNodesNotFound.contains(node))
				configNodesNotFound.add(node);
			this.notifyDataSetChanged();
		}
		
		/**************************************************************************
		 * Removes elements to the configNodesNOtFound vector
		 * @param node The string in the form of what is expected to be seen
		 * 						in the listView(name\nMAC\nlocation) which represents a node.
		 *****************************************************************************/
		public void removeFromConfigNodesNotFound(String node){
			configNodesNotFound.remove(node);
			this.notifyDataSetChanged();
		}
				
		/**************************************************************************
		 * Removes all elements to the configNodesNotFound vector
		 *****************************************************************************/
		public void clearConfigNodesNotFound(){
			configNodesNotFound.clear();
			this.notifyDataSetChanged();
		}
		
		/**************************************************************************
		 * Updates formatted string in configNodesNotFound
		 *****************************************************************************/
		public void updateConfigNodesNotFound(String updateValue){
			String MAC = updateValue.split("\n")[1];
			for(int i = 0; i < configNodesNotFound.size(); i++){
				if(configNodesNotFound.get(i).split("\n")[1].equals(MAC)){
					configNodesNotFound.set(i, updateValue);
					break;
				}
			}
			
		}
		
		/**************************************************************************
		 * Adds elements to the selected vector
		 * @param selectedItem The string in the form of what is expected to be seen
		 * 						in the listView(name\nMAC\nlocation) which represents a node.
		 *****************************************************************************/
		public void addSelected(String selectedItem) {
			if(vals.indexOf(selectedItem) >= 0)
				selected.add(vals.indexOf(selectedItem));
			this.notifyDataSetChanged();
			
		}

		/**************************************************************************
		 * Get a View that displays the data at the 
		 * specified position in the data set.  It, also, specifies what the 
		 * checkbox in the item does.
		 * @param position The position of the item within the adapter's data set 
		 * 					of the item whose view we want.
		 * @param convertView The old view to reuse, if possible.
		 * @parm parent The parent that this view will eventually be attached to
		 * @return A View corresponding to the data at the specified position.
		 *****************************************************************************/
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View rowView = inflater.inflate(R.layout.row, parent, false);
			TextView textView = (TextView) rowView.findViewById(R.id.label);
			final CheckBox select = (CheckBox) rowView
					.findViewById(R.id.select);
			textView.setText(vals.get(position));
			
			
			
			if(selected.contains(position)){
				select.setChecked(true);
			} else {
				select.setChecked(false);
			}
			
			if(configNodesNotFound.contains(vals.get(position))){
				textView.setTextColor(Color.RED);
			} else {
				textView.setTextColor(Color.WHITE);
			}
			
			select.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					// TODO Auto-generated method stub

					if (!select.isChecked()) {
						selected.removeElement(position);
					} else if (selected.size() < 7) {
						selected.addElement(position);
					} else {
						select.setChecked(false);
					}

				}
			});

			return rowView;
		}

		/**************************************************************************
		 * Collects the strings corresponding to the positions
		 * in selected in a list, and returns them to the caller.
		 * @return A vector of strings that contains the displayed items that
		 * 			were selected to be in a session.
		 *****************************************************************************/
		public Vector<String> getSelected() {
			Vector<String> selectedStrings = new Vector<String>();

			for(int i = 0; i < selected.size(); i++){
				selectedStrings.add(vals.get(selected.get(i)));
			}
			return selectedStrings;
		}
		
		

	}
}
