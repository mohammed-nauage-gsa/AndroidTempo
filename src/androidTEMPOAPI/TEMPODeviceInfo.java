package androidTEMPOAPI;



public class TEMPODeviceInfo {

	private String location;
	private String name;
	private String mac;
	
	public TEMPODeviceInfo(String mac, String name) {
		// TODO Auto-generated constructor stub
		this.mac = mac;
		this.name = name;
		
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
		
}
