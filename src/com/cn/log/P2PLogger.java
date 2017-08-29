package com.cn.log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

public class P2PLogger {

	/**
	 * @param msg - msg to log
	 * @param peerId - cur peer id
	 */
	public static void log(String msg, int peerId) {
	        String fName = "log_peer_" + peerId + ".log";
       		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY hh:mm:ss");
	        String dateString = sdf.format(System.currentTimeMillis());
	        msg = "[" + dateString + "]: " + msg;
	        PrintWriter out = null;
	        try{
		       	out = new PrintWriter(new BufferedWriter(new FileWriter(fName, true)));
	        	out.println(msg);
	        } catch (IOException e) {
		        e.getMessage();
	        } finally {
	        	out.close();
		}
        
	}

	public static void log(String msg) {
		if (!msg.isEmpty()) {
			int sepInd = msg.indexOf(":");
			String cPeerId = msg.substring(0, sepInd);
			msg = msg.substring(sepInd+1);
			String fName = cPeerId + ".log";
			PrintWriter out = null;
			try {
				out = new PrintWriter(new BufferedWriter(new FileWriter(fName, true)));
				out.println(msg);
			} catch (IOException e) {
				e.getMessage();
			} finally {
				out.close();
			}

		}

	}
}
