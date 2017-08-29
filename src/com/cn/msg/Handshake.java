package com.cn.msg;

@SuppressWarnings("deprecation")
public class Handshake {

	static String handshakeHeader = "P2PFILESHARINGPROJ";
	
	public static byte[] getHandShakeMsg(int peerId) {

		byte message[] = new byte[32];
		handshakeHeader.getBytes(0, handshakeHeader.length(), message, 0);
		int k = handshakeHeader.length();
		for (int i = 0; i < 10; i++)
			message[k++] = 0;

		String peerid = String.valueOf(peerId);
		int len = (peerid.length());
		len = 4 - len; // no of zeroes required
		for (int i = 0; i < len; i++)
			message[k++] = (byte) '0';

		for (int i = 0; i < peerid.length(); i++)
			message[k++] = (byte) peerid.charAt(i);

		return message;
	}


	public static boolean isHandshake(byte[] bs) {
		if (bs.length != 32){
//			System.out.println("length of message is not 32, Actual length: " + bs.length);
			return false;
		}
		byte header[] = new byte[18];
		byte zeroBits[] = new byte[10];
		byte peerId[] = new byte[4];
		int i, k = 0;
		for (i = 0; i < 18; i++)
			header[k++] = bs[i];
		k = 0;
		for (; i < 28; i++)
			zeroBits[k++] = bs[i];
		k = 0;
		for (; i < 32; i++)
			peerId[k++] = bs[i];

		String str = new String(header);
		if (!str.equals("P2PFILESHARINGPROJ")) {
//		System.out.println("isHandshake: header does not contain string 'P2PFILESHARINGPROJ'");
			return false;
		}
		for (i = 0; i < 10; i++) {
			if (zeroBits[i] != 0){
//				System.out.println("isHandshake: Byte "+(i+1)+" after header is not zero");
				return false;
			}
		}
		for (i = 0; i < 4; i++) {
			if (peerId[i] < 48 || peerId[i] > 57){
//				System.out.println("isHandshake: PeerId is not an integer");
				return false;
			}
		}

		return true;
	}

	public static int getRemotePeerId(byte[] msg){
		
		StringBuffer b = new StringBuffer();
		for(int i=0;i<=3;i++)
			b.append(msg[28+i]-48);
		
		return Integer.parseInt(b.toString());
	}
	
	/* Test isHandshake
	 * public static void main(String args[]) {
		byte bs[] = { 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88,
				89, 90, 97, 98, 99, 100, 101, 102 };
		System.out.println(bs.length);
		isHandshake2(bs);
	}*/

}
