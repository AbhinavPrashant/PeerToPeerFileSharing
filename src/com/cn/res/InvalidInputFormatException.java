package com.cn.res;

public class InvalidInputFormatException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String str;
	public InvalidInputFormatException(String str) {
		this.str = str;
	}
	
	@Override
	public String getMessage() {
		return str+" has more no of tokens as required";
	}
}
