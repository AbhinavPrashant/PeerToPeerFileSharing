package com.cn.connection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.cn.handler.MsgHandler;
import com.cn.log.P2PLogger;
import com.cn.msg.Handshake;
import com.cn.peer.Peer;

public class ClientConnection {
	Peer curPeer;
	Peer remotePeer;
	Socket socket;
	ObjectOutputStream out;
	ObjectInputStream in;

	public ClientConnection(Peer curPeer, Peer remotePeer) {
		this.curPeer = curPeer;
		this.remotePeer = remotePeer;
	}

	public void initiate() {

		try {
			socket = new Socket(remotePeer.getPeerAddr(), remotePeer.getPeerPort());
			P2PLogger.log("Peer "+curPeer.getPeerId()+" makes a connection to Peer "+remotePeer.getPeerId()+".",curPeer.getPeerId());
			curPeer.addSocket(socket);
			//in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();

			new MsgHandler(socket,out, curPeer).start();

			byte msg[] = Handshake.getHandShakeMsg(curPeer.getPeerId());
			sendMessage(msg);
			
			
		} catch (ConnectException e) {
			System.out.println("Connection refused.");
			// TODO: myLogger.message("Connection refused.");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			System.out.println("You are trying to connect to an unknown host!");
			// TODO: myLogger.message("You are trying to connect to an unknown
			// host!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException occurred");
			// TODO: myLogger.message("IOException occurred");
			e.printStackTrace();
		} 
		
	}

	void sendMessage(byte[] msg) {
		synchronized(out){
		try {
			// TODO: Log this myLogger.message("Sending message of length " +
			// msg.length + " bytes");
			out.writeObject(msg);
			out.flush();

		} catch (IOException ioException) {
			System.out.println("IOException occurred");
			// TODO: myLogger.message("IOException occurred");
			ioException.printStackTrace();
		}
		}
	}
}
