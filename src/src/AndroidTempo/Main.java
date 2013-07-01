package src.AndroidTempo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import src.AndroidTempo.TEMPOService.LocalBinder;

import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
	private SelectionAdapter selectionList;

	/**
	 * Contains the string that the user specifies to describe the session.
	 */
	private String sessionDescription;


	/**
	 * Contains the list of annotations expected to be used in the current session
	 */	
	private ArrayList<String> annotationList;

	/**
	 * Contains the node names that are a part of a predefined configuration that has been selected
	 */
	private TEMPODeviceInfo[] configNodes;

	private TEMPOService mService;
	

	/**************************************************************************
	 * Creates default files and folders if they do not exist
	 *****************************************************************************/
	private void createDefaultFiles(){
		
		if (!configurationFolder.exists() || !configurationFolder.isDirectory()) {
			configurationFolder.mkdir();
		}

		// Checks if a temp folder exists and if not it makes it
		if (!tempDir.isDirectory())
			tempDir.mkdir();
		
	}

	/**************************************************************************
	 * Initializes member fields
	 * @param savedInstanceState holds all the data that may be saved when
	 * 							onSaveInstanceState
	 *****************************************************************************/
	private void initVars(){

		// which will store the description of the session.
		sessionDescription = "";

		annotationList = new ArrayList<String>();
		configNodes = new TEMPODeviceInfo[0];
		
		
		// initializes an empty ListView
		ListView devicesFound = (ListView) findViewById(R.id.deviceList);
		selectionList = new SelectionAdapter(
				devicesFound.getContext(), new ArrayList<TEMPODeviceInfo>());
		devicesFound.setAdapter(selectionList);
		devicesFound.setOnItemClickListener(new ItemClickListener());
		
		
		
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
		
		Intent connectionServiceIntent = new Intent(this,
				TEMPOService.class);
		startService(connectionServiceIntent);
		
		// bind to connection service
		// so functions in the service can be accessed
		connectionServiceIntent = new Intent(this,
				TEMPOService.class);
		bindService(connectionServiceIntent, mConnection,
				Context.BIND_AUTO_CREATE);
		createDefaultFiles();
		
		initVars();

	}

	/**************************************************************************
	 * Deallocates resources for this activity
	 *****************************************************************************/
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopService(new Intent(Main.this, TEMPOService.class));
		unbindService(mConnection);
		
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
			final TEMPODeviceInfo item = (TEMPODeviceInfo) adpt.getItemAtPosition(pos);
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
			input.setText(item.getLocation());
			// adds it to the builder
			alert.setView(input);
			// sets the default message of the alert box
			alert.setMessage("Enter Location for "
					+ item.getNodeName());
			
			// sets what happens when you press OK
			alert.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// gets the location text and replaces new
							// lines with spaces for the location text
							String loc = input.getText().toString()
									.replace("\n", " ");


							item.setLocation(loc);
							
							// updates ListView
							selectionList.notifyDataSetChanged();
							
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

		ArrayList<TEMPODeviceInfo> selected = selectionList.getSelected();

		if(selected.size() > 0){
			// sends Monitor the set of nodes that were selected for the session
			Intent i = new Intent(Main.this, SessionMonitor.class);
			Bundle b = new Bundle();
			i.putExtra("selected", selected);
			Collections.sort(annotationList);
			i.putExtra("annotations", annotationList);
			try {
				// writes names and locations of the selected nodes into
				// a file which will be later used by datToXML.java
				FileWriter nameAndLocWriter = new FileWriter(new File(tempDir,
						"namesAndLocations.dat"));
				//String name;
				for (int j = 0; j < selected.size(); j++) {
					nameAndLocWriter.write(selected.get(j).getNodeName() + "\n"
							+ selected.get(j).getLocation() + "\n");
	
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
		
			Toast toast = Toast.makeText(getApplicationContext(), "Please Select Nodes", Toast.LENGTH_LONG);
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
					String[] mac,loc,anno;
					File configFile = new File(configurationFolder, input.getSelectedItem().toString());
					BufferedReader configFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
					mac = configFileReader.readLine().replace("\n", "").split(",");					
					loc = configFileReader.readLine().replace("\n", "").split(",");
					System.out.println(loc.length);
					System.out.println(mac.length);
					configNodes = new TEMPODeviceInfo[mac.length];
					for(int i = 0; i < mac.length; i++){
						configNodes[i] = mService.getTEMPODevice(mac[i]);
						configNodes[i].setLocation(loc[i]);
					}
					
					
					anno = configFileReader.readLine().replace("\n", "").split(",");
					annotationList = new ArrayList<String>();
					for(int i = 0; i < anno.length; i++){
						annotationList.add(anno[i]);
					}
					
					
					sessionDescription = configFileReader.readLine().replace("\n","");
					
					selectionList.clearSelected();
					selectionList.clear();
					Arrays.sort(configNodes);
					addConfigurationNodes();
					
					
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	 * Adds configuration nodes to the ListView
	 * @param view The view that was clicked
	 *****************************************************************************/
	private void addConfigurationNodes(){
		TEMPODeviceInfo[] localConfigNodes = configNodes;
		selectionList.resetConfigNodeAvailailability();
		for(int i = 0; i < localConfigNodes.length; i++){
			selectionList.add(localConfigNodes[i]);
			selectionList.setConfigNodeAvailable(localConfigNodes[i], false);
			selectionList.setSelected(localConfigNodes[i], true);

		}
		
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
					ArrayList<TEMPODeviceInfo> selected = selectionList.getSelected();
					if(selected != null && selected.size() > 0){
						
						for(int i = 0; i < selected.size()-1; i++){
							saveData += selected.get(i).getMac() + ",";
						}
						saveData += selected.get(selected.size()-1).getMac() + "\n";

						for(int i = 0; i < selected.size()-1; i++){
							saveData += selected.get(i).getLocation() + ",";							
						}
						
						saveData += selected.get(selected.size()-1).getLocation() + "\n";

						
					}

					if(annotationList != null){
						String annotationListString = Arrays.toString(annotationList.toArray());
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

		// clears everything that may be changed due to searching for
		// bluetooth devices
		selectionList.clear();
		selectionList.clearSelected();
		addConfigurationNodes();
		ArrayList<TEMPODeviceInfo> availableNodes = mService.getAvailableNodes();
		for(TEMPODeviceInfo i: availableNodes){
			if (selectionList.getPosition(i) == -1) {
				// if it is a tempo node and it does not exist in the
				// list view it is added to the ListView
				selectionList.add(i);
			}
			if(Arrays.binarySearch(configNodes, i) >= 0){
				selectionList.setConfigNodeAvailable(i, true);
			}
		}

		selectionList.notifyDataSetChanged();

	}
	

	

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
			selectionList.clear();
			selectionList.clearSelected();
			selectionList.notifyDataSetChanged();


			File[] files = tempDir.listFiles();

			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
			tempDir.delete();
			tempDir = new File(rootDir, "temp");
			tempDir.mkdir();


		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		  super.onSaveInstanceState(savedInstanceState);
		  savedInstanceState.putParcelableArray("configNodes", configNodes);
		  ArrayList<TEMPODeviceInfo> selectedList = selectionList.getSelected();
		  
		  savedInstanceState.putParcelableArray("selectedNodes", selectedList.toArray(new TEMPODeviceInfo[selectedList.size()]));
		  savedInstanceState.putParcelableArray("unavailableConfigNodes", selectionList.getUnavailableConfigNodes());
		  savedInstanceState.putSerializable("allItems", selectionList.getAllItems());		  
		  
		  savedInstanceState.putStringArrayList("annotationsList", annotationList);
		  savedInstanceState.putString("sessionDescription", sessionDescription);
		  
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);

	  configNodes =  (TEMPODeviceInfo[]) savedInstanceState.getParcelableArray("configNodes");
	  //addConfigurationNodes();
	  TEMPODeviceInfo[] allItems = (TEMPODeviceInfo[]) savedInstanceState.getParcelableArray("allItems");
	  
	  for(int i = 0; i < allItems.length; i++){
		  selectionList.add(allItems[i]);
	  }
	  
	  TEMPODeviceInfo[] unavailableConfigNodes = (TEMPODeviceInfo[]) savedInstanceState.getParcelableArray("unavailableConfigNodes");
	  for(int i = 0; i < unavailableConfigNodes.length; i++){
		  selectionList.setConfigNodeAvailable(unavailableConfigNodes[i], false);
	  }
	  TEMPODeviceInfo[] selectedNodes = (TEMPODeviceInfo[]) savedInstanceState.getParcelableArray("selectedNodes");
	  for(int i = 0; i < selectedNodes.length; i++){
		  selectionList.setSelected(selectedNodes[i], true);
	  }
	  
	  selectionList.notifyDataSetChanged();
	  
	  annotationList = savedInstanceState.getStringArrayList("annotationsList");
	  sessionDescription = savedInstanceState.getString("sessionDescription");
	}
	
	
	
	
	/**************************************************************************
	 * This class keeps track of information concerning a ListView(what is
	 * or is not selected and what exists in a ListView).
	 *****************************************************************************/
	private class SelectionAdapter extends ArrayAdapter<TEMPODeviceInfo> {

		private final Context context;
		private ArrayList<TEMPODeviceInfo> allItems, unavailableConfigNodes;
		private Vector<Integer> selected;

		/**************************************************************************
		 * Constructs an instance of SelectionAdapter that sets the values
		 * @param context The current context.
		 * @param pValues The list of strings that will initially be in the ListView.
		 *****************************************************************************/
		public SelectionAdapter(Context context, ArrayList<TEMPODeviceInfo> pValues) {
			super(context, R.layout.row, pValues);
			this.context = context;
			allItems = pValues;
			selected = new Vector<Integer>();
			unavailableConfigNodes = new ArrayList<TEMPODeviceInfo>();
		}
		public TEMPODeviceInfo[] getAllItems() {
			// TODO Auto-generated method stub
			return allItems.toArray(new TEMPODeviceInfo[allItems.size()]);
		}
		public TEMPODeviceInfo[] getUnavailableConfigNodes() {
			// TODO Auto-generated method stub
			return unavailableConfigNodes.toArray(new TEMPODeviceInfo[unavailableConfigNodes.size()]);
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
		 * Removes elements to the configNodesNOtFound vector
		 * @param node The string in the form of what is expected to be seen
		 * 						in the listView(name\nMAC\nlocation) which represents a node.
		 *****************************************************************************/
		public void setConfigNodeAvailable(TEMPODeviceInfo node, boolean available){
			
			if(available)
				unavailableConfigNodes.remove(node);
			else if(!unavailableConfigNodes.contains(node))
				unavailableConfigNodes.add(node);
			
			this.notifyDataSetChanged();
		}
				
		/**************************************************************************
		 * Removes all elements to the unavailableConfigNodes vector
		 *****************************************************************************/
		public void resetConfigNodeAvailailability(){
			unavailableConfigNodes.clear();
			this.notifyDataSetChanged();
		}
		
		
		/**************************************************************************
		 * Adds elements to the selected vector
		 * @param selectedItem The string in the form of what is expected to be seen
		 * 						in the listView(name\nMAC\nlocation) which represents a node.
		 *****************************************************************************/
		public boolean setSelected(TEMPODeviceInfo node, boolean isSelected) {
			boolean ret = false;
			if(allItems.indexOf(node) >= 0 && selected.size() < 7 && isSelected) {
				selected.add(allItems.indexOf(node));
				ret = true;
			} else if(allItems.contains(node) && !isSelected) {
				selected.removeElement(allItems.indexOf(node));
				ret = true;
			}
			this.notifyDataSetChanged();
			return ret;
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
			final CheckBox select = (CheckBox) rowView
					.findViewById(R.id.select);
			textView.setText(allItems.get(position).toString());
			
			
			
			if(selected.contains(position)){
				select.setChecked(true);
			} else {
				select.setChecked(false);
			}
			
			if(unavailableConfigNodes.contains(allItems.get(position))){
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
		public ArrayList<TEMPODeviceInfo> getSelected() {
			ArrayList<TEMPODeviceInfo> selectedStrings = new ArrayList<TEMPODeviceInfo>();

			for(int i = 0; i < selected.size(); i++){
				selectedStrings.add(allItems.get(selected.get(i)));
			}
			return selectedStrings;
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
			
			


		}

		public void onServiceDisconnected(ComponentName arg0) {

		}
	};
}
