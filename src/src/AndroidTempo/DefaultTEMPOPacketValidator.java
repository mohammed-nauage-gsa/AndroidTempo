package src.AndroidTempo;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DefaultTEMPOPacketValidator {

	
	private final static byte MOD_LF = '\n' + 0x10;	
	private final static byte MOD_CR = '\r' + 0x10;
	
	public static TEMPOPacket validate(TEMPOPacket currentPacket, TEMPOPacket previousPacket){
		
		if(currentPacket == null)
			return null;
		
		byte[] currentPacketBytes = checkData(currentPacket.getByteBufferOfPacket().array());
		Integer currentSysCounter = currentPacket.getSysCounter();
		
		if(currentSysCounter == null)
			return null;
		
		int currentPacketLength = currentPacketBytes.length;
		
		if(previousPacket == null)
			return new TEMPOPacket(ByteBuffer.wrap(currentPacketBytes), currentPacket.getTimeRecieved());
		
		Integer previousSysCounter = previousPacket.getSysCounter();
		
		if(previousSysCounter == null)
			return null;
		
		ByteBuffer zeroPacketBuffer = ByteBuffer.allocate(128*((int)previousSysCounter - currentSysCounter) + 6);
		zeroPacketBuffer.putInt(zeroPacketBuffer.capacity()-6, currentPacket.getSysCounter());
		zeroPacketBuffer.putChar(zeroPacketBuffer.capacity()-2, '\r');
		zeroPacketBuffer.putChar(zeroPacketBuffer.capacity()-1, '\n');

		TEMPOPacket zeroPacket = new TEMPOPacket(zeroPacketBuffer, currentPacket.getTimeRecieved());
		
		TEMPOPacket validatedPacket = null;
		
		if (currentPacketLength <= 6 || currentPacketBytes[(currentPacketLength - 2)] != '\r' || currentPacketBytes[(currentPacketLength - 1)] != '\n'
				|| (currentPacketLength - 6) % TEMPOPacket.BYTES_PER_SAMPLE != 0) {

			return zeroPacket;
					
		} else {


			//gets the difference in the expected number of 
			//data points recieved and the actual number
			int diff = (int) (6 * (currentSysCounter - previousSysCounter) - (currentPacketLength - 6));
			int validatedPacketLength = currentPacketLength + (int) (TEMPOPacket.BYTES_PER_SAMPLE * Math.floor(diff / TEMPOPacket.BYTES_PER_SAMPLE));

			byte[] validatedDataArray = null;
			//adds diff to a varialbe which keeps track of 
			//the number of bytes the current data set is off by
			if (diff <= TEMPOPacket.BYTES_PER_SAMPLE && diff >= -TEMPOPacket.BYTES_PER_SAMPLE) {
				//if the number of bytes the current data set is 
				//off by is within acceptable ranges
				//it sets a variable to accept all that data from the packet
				validatedPacket = currentPacket;

			} else if (diff > TEMPOPacket.BYTES_PER_SAMPLE) {
				//If the data set has fewer points than allowable,
				//it pads the current data packet with 0's
				
				validatedDataArray = Arrays.copyOf(currentPacketBytes, validatedPacketLength);
				
				for (int i = currentPacketLength - 6; i < validatedPacketLength-6; i++) {
					validatedDataArray[i] = 0;
				}

			} else if (diff < -TEMPOPacket.BYTES_PER_SAMPLE) {
				//If the data set has more points than allowable,
				//it only accepts a portion of the data.
				validatedDataArray = Arrays.copyOf(currentPacketBytes, validatedPacketLength);
			

			}
			
			if(validatedDataArray != null){
				ByteBuffer validatedPacketBuffer = ByteBuffer.wrap(validatedDataArray);
				validatedPacketBuffer.putInt(validatedPacketBuffer.capacity()-6, currentPacket.getSysCounter());
				validatedPacketBuffer.putChar(validatedPacketBuffer.capacity()-2, '\r');
				validatedPacketBuffer.putChar(validatedPacketBuffer.capacity()-1, '\n');				
				validatedPacket = new TEMPOPacket(validatedPacketBuffer, currentPacket.getTimeRecieved());
				
			}
		}

		return validatedPacket;

	}

	private static byte[] checkData(byte[] packetBytes){
		byte mcr = MOD_CR;
		byte mlf = MOD_LF;
		
		//makes sure that if 0x10 was added by the to prevent 
		//a byte from being a \r or \n it is set to the correct value
		
		for (int i = 0; i < packetBytes.length - 6; i += 2) {
			if (packetBytes[i] == mlf || packetBytes[i] == mcr) {
				packetBytes[i] -= 0x10;
			}
			if (packetBytes[i] >= 0x10) {
				packetBytes[i] = 0;
				packetBytes[i+1] = 0;
			}
		}
		return packetBytes;
	}
	

	
}
