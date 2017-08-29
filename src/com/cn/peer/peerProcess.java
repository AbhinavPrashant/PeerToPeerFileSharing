package com.cn.peer;

import java.util.LinkedList;

import com.cn.config.PeerInfoReader;
import com.cn.connection.ClientConnection;
import com.cn.connection.ServerConnectionAcceptor;
import com.cn.handler.ChokeHandler;
import com.cn.handler.OptimisticUnchokeHandler;
import com.cn.res.IConstants;

public class peerProcess {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("Error: PeerID missing from command\n\nUsage: java peerProcess <peerID>");
			return;
		}
		//TODO: check whether args[0] has the port or args[1]
		int curPeerId = Integer.parseInt(args[0]);
		PeerInfoReader pr = new PeerInfoReader(IConstants.PEER_INFO_FILENAME);
		
		Peer curPeer = pr.getPeerFromPeerId(curPeerId);
		new ServerConnectionAcceptor(curPeer).start();
		new ChokeHandler(curPeer).start();
		new OptimisticUnchokeHandler(curPeer).start();
		
		LinkedList<Peer> list = pr.getPeerList(); // all the peers in the file

		for(Peer remotePeer: list){
			if(remotePeer.getPeerId() == curPeerId )// stop opening client connections
				break;
			ClientConnection c = new ClientConnection(curPeer, remotePeer);
			c.initiate();
		}
		while(!curPeer.haveAllOtherFilesDownloaded()){	
			Thread.sleep(1000);
		}
		Thread.sleep(1000);
		System.out.println("---Everyone has the file. Exiting!---");
		System.exit(0);
		
	}
}
