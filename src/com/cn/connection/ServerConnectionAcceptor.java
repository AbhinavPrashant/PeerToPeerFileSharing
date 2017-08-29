package com.cn.connection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.cn.handler.MsgHandler;
import com.cn.msg.Handshake;
import com.cn.peer.Peer;
import com.cn.res.IConstants;

import java.net.*;
import java.io.*;


public class ServerConnectionAcceptor extends Thread {
	private Peer curPeer;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public ServerConnectionAcceptor(Peer curPeer) {
		this.curPeer = curPeer;
	}

	public void run() {
		int sPort = curPeer.getPeerPort();
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(sPort);
			System.out.println("peerProcess: Server socket opened for peer {" + curPeer.getPeerString() + "}");
			int numConnections=1;
			while (true) {
				Socket socket = listener.accept();
				//Uncomment this when running on multiple machines
				//P2PLogger.log("Peer "+curPeer.getPeerId()+" is connected from Peer "+getRemotePeerID(socket)+".",curPeer.getPeerId());				
				//in = new ObjectInputStream(socket.getInputStream());
				out = new ObjectOutputStream(socket.getOutputStream());
				out.flush();

				new MsgHandler(socket,out, curPeer).start();
				curPeer.addSocket(socket); // TODO: Verify this

			//	out = new ObjectOutputStream(socket.getOutputStream());
			
				byte msg[] = Handshake.getHandShakeMsg(curPeer.getPeerId());
				sendMessage(msg);
				numConnections++;
			}
		} catch (IllegalArgumentException e) {
			System.out.println("peerProcess: Port must be in range 0-65535 for peer {" + curPeer.getPeerString() + "}");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("peerProcess: Cannot open socket at port " + sPort + "(Port may not be free)");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("peerProcess: Unknown exception caught while opening server socket");
			e.printStackTrace();
		} finally {
			try {
				if (listener != null)
					listener.close();
			} catch (IOException e) {
				System.out.println("peerProcess: Unable to close server socket");
				e.printStackTrace();
			}
		}
	}

	void sendMessage(byte[] msg) {
		try {
			System.out.println("Sending message of length " + msg.length + " bytes");
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

	private int getRemotePeerID(Socket socket) throws FileNotFoundException, IOException {
		InetAddress inetAddress = socket.getInetAddress();
		String hostName =  "";
		String ipAdd = "";
		String port = "";
		if(inetAddress != null){
			hostName = inetAddress.getHostName();
			port = String.valueOf(socket.getPort());
			ipAdd = inetAddress.getHostAddress();
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(IConstants.PEER_INFO_FILENAME));
		BufferedReader in = new BufferedReader(reader);
		String line = "";
		int remotePeerId = -1;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if ((line.length() <= 0) || (line.startsWith(IConstants.COMMENT_CHAR))) {
				continue;
			}
			
			String[] split = line.split("\\s+");
			String ipOrHost = split[1].toLowerCase();
			System.out.println("Port is -"+port + "- split is -"+split[2]+"-");
			if(!ipOrHost.isEmpty() && ( hostName.toLowerCase().contains(ipOrHost) || ipAdd.toLowerCase().contains(ipOrHost) ) && split[2].contains(port)){
				remotePeerId = Integer.parseInt(split[0]);
				break;
			}
		}
		
		return remotePeerId;
	}
	
}
