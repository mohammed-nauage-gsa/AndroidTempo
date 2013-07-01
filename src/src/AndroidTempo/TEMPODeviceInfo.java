package src.AndroidTempo;

import android.os.Parcel;
import android.os.Parcelable;



public class TEMPODeviceInfo implements Parcelable{

	private String location;
	private String name;
	private String mac;
	
	public static final Parcelable.Creator<TEMPODeviceInfo> CREATOR = new Parcelable.Creator<TEMPODeviceInfo>() {
		public TEMPODeviceInfo createFromParcel(Parcel in) {
			return new TEMPODeviceInfo(in);
		}

		public TEMPODeviceInfo[] newArray(int size) {
			return new TEMPODeviceInfo[size];
		}
	};
	
	public TEMPODeviceInfo(String mac, String name) {
		// TODO Auto-generated constructor stub
		this.mac = mac;
		this.name = name;
		
	}


	public TEMPODeviceInfo(Parcel in) {
		// TODO Auto-generated constructor stub
		name = in.readString();
		mac = in.readString();
		location = in.readString();
	}


	/**************************************************************************
	 * MAC getter
	 * @return the MAC value
	 *****************************************************************************/
	public String getMac() {
		return mac;
	}

	/**************************************************************************
	 * name getter
	 * @return the name value
	 *****************************************************************************/
	public String getNodeName() {
		return name;
	}

	public String getLocation() {
		// TODO Auto-generated method stub
		return location;
	}

	public void setLocation(String loc) {
		// TODO Auto-generated method stub
		location = loc;
	}
	
	public String toString(){
		return name
				+ "\n"
				+ mac
				+ "\n"
				+ (location == null ? "" : location);
	}


	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}


	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(name);
		dest.writeString(mac);
		dest.writeString(location);
		
	}
		
}
