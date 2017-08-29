package com.cn.msg;

public class Message {

	public int msgLen;
	public byte msgType;
	public byte [] msgPayload;
	public Message(byte msgType) {
		super();
		this.msgType = msgType;
	}
	
}
