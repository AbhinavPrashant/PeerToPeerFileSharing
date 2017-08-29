package com.cn.msg;

import java.nio.ByteBuffer;

public class NotInterested {

	public static byte[] getNotInterestedMsg() {
		int msgLen = 1;
		int msgType = 8;
		byte[] msg = new byte[5];
		byte[] tempMsg = ByteBuffer.allocate(4).putInt(msgLen).array();
		int i = 0;
		for (i = 0; i < 4; i++) {
			msg[i] = tempMsg[i];
		}
		msg[i] = (byte) msgType;
		return msg;
	}

}
