package src.AndroidTempo;

import java.nio.ByteBuffer;

public class TEMPOPacket {

	public final static int BYTES_PER_SAMPLE = 12;

	
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


	public short[] getXaccel(){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.capacity()-6)%12 == 0){
			short[] Xaccel = new short[packet.capacity()/12];
			for(int i = 0; i < packet.capacity()-6; i+=12){
				Xaccel[i/12] = packet.getShort(i);
			}
			return Xaccel;
		} else {
			return null;
		}
		
		
	}

	public short[] getYaccel(){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.capacity()-6)%12 == 0){
			short[] Yaccel = new short[packet.capacity()/12];
			for(int i = 2; i < packet.capacity()-6; i+=12){
				Yaccel[i/12] = packet.getShort(i);
			}
			return Yaccel;
		} else {
			return null;
		}
		
		
	}
	public short[] getZaccel(){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.capacity()-6)%12 == 0){
			short[] Zaccel = new short[packet.capacity()/12];
			for(int i = 4; i < packet.capacity()-6; i+=12){
				Zaccel[i/12] = packet.getShort(i);
			}
			return Zaccel;
		} else {
			return null;
		}
		
		
	}
	public short[] getXGyro(){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.capacity()-6)%12 == 0){
			short[] Xgyro = new short[packet.capacity()/12];
			for(int i = 6; i < packet.capacity()-6; i+=12){
				Xgyro[i/12] = packet.getShort(i);
			}
			return Xgyro;
		} else {
			return null;
		}
		
		
	}
	public short[] getYGyro(){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.capacity()-6)%12 == 0){
			short[] Ygyro = new short[packet.capacity()/12];
			for(int i = 8; i < packet.capacity()-6; i+=12){
				Ygyro[i/12] = packet.getShort(i);
			}
			return Ygyro;
		} else {
			return null;
		}
		
		
	}
	
	public short[] getZgyro(){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.capacity()-6)%12 == 0){
			short[] Zgyro = new short[packet.capacity()/12];
			for(int i = 10; i < packet.capacity()-6; i+=12){
				Zgyro[i/12] = packet.getShort(i);
			}
			return Zgyro;
		} else {
			return null;
		}
		
		
	}
	
	
	public Integer getSysCounter(){
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		if((packet.capacity()-6)%12 == 0){
			return packet.getInt(packet.capacity()-6);
		} else {
			return null;
		}
		
		
	}
	
	public short[][] getAllData(){
		
		
		//ArrayList<Short> Xaccel, Yaccel, Zaccel, Xgyro, Ygyro, Zgyro;
		
		if((packet.capacity()-6)%12 == 0){
			short[][] data = new short[6][packet.capacity()/12];
			for(int i = 0; i < packet.capacity()-6; i+=2){
				data[(i/2)%6][i/12] = packet.getShort(i);
			}
			return data;
		} else {
			return null;
		}
	
	}
	
	
}
