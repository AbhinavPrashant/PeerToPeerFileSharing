package com.cn.msg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Have {
	
	public static byte[] getHaveMsg(int pieceIndex) {

		int msgLen = 5; // 1 byte msgtype + 4 byte piece index
		int msgType = 16;
		byte[] msg = new byte[4 + msgLen]; // 4 byte message length
		byte[] tempLen = ByteBuffer.allocate(4).putInt(msgLen).array();
		int i = 0;
		for (i = 0; i < 4; i++) {
			msg[i] = tempLen[i];
		}
		msg[i++] = (byte) msgType;
		
		byte tempIdx[]=ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceIndex).array();
		   
		
		for(;i<9;i++){
			msg[i]=tempIdx[i-5];
		}
			
		return msg;
	}

	public static int getPieceIndex(byte msg[]) {
		return ByteBuffer.wrap(Arrays.copyOfRange(msg, 5, 9)).order(ByteOrder.BIG_ENDIAN).getInt();
	}
	
/*	public static void main(String ar[]){
		byte msg[] = Have.getHaveMsg(255);;
		System.out.println(Have.getPieceIndex(msg));
		msg = Have.getHaveMsg(256);;
		System.out.println(Have.getPieceIndex(msg));
		msg = Have.getHaveMsg(257);;
		System.out.println(Have.getPieceIndex(msg));
		msg = Have.getHaveMsg(258);;
		System.out.println(Have.getPieceIndex(msg));
		msg = Have.getHaveMsg(259);;
		System.out.println(Have.getPieceIndex(msg));
		
	}*/
}
