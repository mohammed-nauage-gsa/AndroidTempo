package src.AndroidTempo;

import java.nio.ByteBuffer;

public class TEMPOPacket {

	private ByteBuffer packet;
	private long timeRecieved;
	
	public TEMPOPacket(ByteBuffer packet, long timeRecieved){
		this.packet = packet;
		this.timeRecieved = timeRecieved;
	}
	
	

	
	public TEMPOPacket(TEMPOPacket p) {
		// TODO Auto-generated constructor stub
		packet = p.getByteBufferOfPacket();
		timeRecieved = p.getTimeRecieved();
	}




	public long getTimeRecieved() {
		// TODO Auto-generated method stub
		return timeRecieved;
	}




	public ByteBuffer getByteBufferOfPacket() {
		// TODO Auto-generated method stub
		return cloneByteBuffer(packet);
	}


	private ByteBuffer cloneByteBuffer(ByteBuffer original) {
		ByteBuffer clone = ByteBuffer.allocate(original.capacity());
		original.rewind();
		clone.put(original);
		original.rewind();
		clone.flip();
		return clone;
	}


	public static short[] getXaccel(byte[] packet){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.length-6)%12 == 0){
			short[] Xaccel = new short[packet.length/12];
			ByteBuffer converter = ByteBuffer.allocate(packet.length);
			converter.put(packet);
			for(int i = 0; i < packet.length-6; i+=12){
				Xaccel[i/12] = converter.getShort(i);
			}
			return Xaccel;
		} else {
			return null;
		}
		
		
	}

	public static short[] getYaccel(byte[] packet){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.length-6)%12 == 0){
			short[] Yaccel = new short[packet.length/12];
			ByteBuffer converter = ByteBuffer.allocate(packet.length);
			converter.put(packet);
			for(int i = 2; i < packet.length-6; i+=12){
				Yaccel[i/12] = converter.getShort(i);
			}
			return Yaccel;
		} else {
			return null;
		}
		
		
	}
	public static short[] getZaccel(byte[] packet){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.length-6)%12 == 0){
			short[] Zaccel = new short[packet.length/12];
			ByteBuffer converter = ByteBuffer.allocate(packet.length);
			converter.put(packet);
			for(int i = 4; i < packet.length-6; i+=12){
				Zaccel[i/12] = converter.getShort(i);
			}
			return Zaccel;
		} else {
			return null;
		}
		
		
	}
	public static short[] getXGyro(byte[] packet){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.length-6)%12 == 0){
			short[] Xgyro = new short[packet.length/12];
			ByteBuffer converter = ByteBuffer.allocate(packet.length);
			converter.put(packet);
			for(int i = 6; i < packet.length-6; i+=12){
				Xgyro[i/12] = converter.getShort(i);
			}
			return Xgyro;
		} else {
			return null;
		}
		
		
	}
	public static short[] getYGyro(byte[] packet){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.length-6)%12 == 0){
			short[] Ygyro = new short[packet.length/12];
			ByteBuffer converter = ByteBuffer.allocate(packet.length);
			converter.put(packet);
			for(int i = 8; i < packet.length-6; i+=12){
				Ygyro[i/12] = converter.getShort(i);
			}
			return Ygyro;
		} else {
			return null;
		}
		
		
	}
	
	public static short[] getZgyro(byte[] packet){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.length-6)%12 == 0){
			short[] Zgyro = new short[packet.length/12];
			ByteBuffer converter = ByteBuffer.allocate(packet.length);
			converter.put(packet);
			for(int i = 10; i < packet.length-6; i+=12){
				Zgyro[i/12] = converter.getShort(i);
			}
			return Zgyro;
		} else {
			return null;
		}
		
		
	}
	
	
	public static Integer getsysCounter(byte[] packet){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.length-6)%12 == 0){
			return ByteBuffer.allocate(packet.length).put(packet).getInt(packet.length-6);
		} else {
			return null;
		}
		
		
	}
	
	public static short[][] getAllData(byte[] packet){
		
		
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.length-6)%12 == 0){
			short[][] data = new short[6][packet.length/12];
			ByteBuffer converter = ByteBuffer.allocate(packet.length);
			converter.put(packet);
			for(int i = 0; i < packet.length-6; i+=12){
				data[(i/2)%6][i/12] = converter.getShort();
			}
			return data;
		} else {
			return null;
		}
	
	}
	
	
}
