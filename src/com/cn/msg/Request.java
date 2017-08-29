package com.cn.msg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Request extends Message {
	private static final byte msgType = 16;
	private static final int msgLen = 5;

	public Request() {
		super(msgType);
		super.msgLen = msgLen;
	}

	public static byte[] getRequestMsg(int pieceIndex) {

		int msgLen = 5; // 1 byte msgtype + 4 byte piece index
		int msgType = 64;
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
	

}
