package com.cn.handler;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

import com.cn.config.CommonConfigEnum;
import com.cn.log.P2PLogger;
import com.cn.msg.Choke;
import com.cn.msg.Unchoke;
import com.cn.peer.Peer;

public class OptimisticUnchokeHandler extends Thread {

	private Peer currPeer;
	private int optimisticInterval;// in msec

	public OptimisticUnchokeHandler(Peer currPeer) {
		this.currPeer = currPeer;
		this.optimisticInterval = Integer.parseInt(
				currPeer.getProperties().get(CommonConfigEnum.OptimisticUnchokingInterval.toString()).toString())
				* 1000;
	}

	public void run() {

		while (true) {

			synchronized(currPeer){

				// Getting all the interested peers
				ArrayList<Integer> intPeers = new ArrayList<Integer>(currPeer.getInterestedPeers());

				if (!intPeers.isEmpty()) {

					// Removing all the peers which are already unchocked
					intPeers.removeAll(currPeer.getUnchokedPeers());

					if (!intPeers.isEmpty()) {
						// shuffle all the contenders for optimistic neighbor
						//Collections.shuffle(intPeers);

						// of the shuffled list get the one with zero index
						Random random = new Random();
                        			int randInd = random.nextInt(intPeers.size());

						int optiUnchockedPeer = intPeers.get(randInd);
						if(currPeer.getOptimiticallyUnchokedPeer()!=-1 &&  !currPeer.getUnchokedPeers().contains(optiUnchockedPeer)){
							if(currPeer.getOptimiticallyUnchokedPeer()!=optiUnchockedPeer)
							{
								byte[] chokeMsg = Choke.getChokeMsg();
								System.out.println("Choking OLD optimistic unchoked peer "+currPeer.getOptimiticallyUnchokedPeer());
								sendMessage(currPeer.getOtherPeerInfo().get(currPeer.getOptimiticallyUnchokedPeer()).out,chokeMsg);
								// set optimistically unchocked peer
								// currPeer.setOptimiticallyUnchokedPeer(optiUnchockedPeer);
							}
						}

						// send unchoke message to the peer
						if(currPeer.getOptimiticallyUnchokedPeer()!=optiUnchockedPeer){
							byte[] unchokeMsg = Unchoke.getUnchokeMsg();
							System.out.println("Sending optimistic unchoking message to peer " + optiUnchockedPeer);
							sendMessage(currPeer.getOtherPeerInfo().get(optiUnchockedPeer).out, unchokeMsg);
							// set optimistically unchocked peer
							currPeer.setOptimiticallyUnchokedPeer(optiUnchockedPeer);
							P2PLogger.log("Peer "+currPeer.getPeerId()+" has the optimistically unchoked neighbor "+optiUnchockedPeer+".",currPeer.getPeerId());
						}
						else{
							System.out.println("Old optimistically unchoked peer selected again, no unchoke message sent");
						}


					}	
				}
			}
			try {
				Thread.sleep(optimisticInterval);
			} catch (InterruptedException e) {
				System.out.println("Error in sleeping optimistic chocking interval thread");
				e.printStackTrace();
			}
		}
	}

	void sendMessage(ObjectOutputStream out, byte[] msg) {
		synchronized(out){
			try {
	//			System.out.println("Sending message of length " + msg.length + " bytes");
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
