package com.cn.handler;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import com.cn.config.CommonConfigEnum;
import com.cn.log.P2PLogger;
import com.cn.msg.Choke;
import com.cn.msg.Unchoke;
import com.cn.peer.Peer;


public class ChokeHandler extends Thread {

	private Peer currPeer;
	private int timer;// in msec

	public ChokeHandler(Peer currP) {
		this.currPeer = currP;
		timer = Integer.parseInt(currPeer.getProperties().get(CommonConfigEnum.UnchokingInterval.toString()).toString())
				* 1000;
	}

	public void run() {
	//	System.out.println("Choke/Unchoke thread started");
		int preferredNbrCount = Integer.parseInt(
				currPeer.getProperties().get(CommonConfigEnum.NumberOfPreferredNeighbors.toString()).toString());

		while (true) {

			synchronized(currPeer){
				// Interested Peer List
				ArrayList<Integer> intPeerList = new ArrayList<>(currPeer.getInterestedPeers());
				for(int interestedPeerId:intPeerList){
					Peer.PeerInfo peerInfo = currPeer.getOtherPeerInfo().get(interestedPeerId);
					long bytes = peerInfo.getBytesSinceUnchoke();
					long curTime = Calendar.getInstance().getTimeInMillis();
					long prevTime = peerInfo.getUnchokeStartTime();
					long deltaTime = curTime-prevTime;
					double calculatedDownloadRate = (double)bytes/(double)deltaTime;
					System.out.println("Peer Id "+interestedPeerId+ " delta time:"+deltaTime + " bytes:"+bytes + " downloadRate:"+calculatedDownloadRate);
					peerInfo.setDownloadRate(calculatedDownloadRate);
				}
				if (intPeerList.size() > 0) {
				//	System.out.println("Interested peer list size is not 0");
					// TODO: set hasFile to true in Peer class if all the pieces
					// have
					// been received
					if (currPeer.isHasFile()) {
						// if the peer has complete file then select preferred
						// neighbor
						// randomly
						System.out.println("Current peer has complete file: chossing peers to unchoke randomly, Peer list is: ");
						for(int j: intPeerList)
							System.out.print(j+",");
						Collections.shuffle(intPeerList);
	
						sendChokeUnchoke(intPeerList, preferredNbrCount);
	
					} else { // currPeer does not have complete file
	
						System.out.println("Chossing choke/unchoke peers based on download rate");
						Collections.shuffle(intPeerList);
	
						Collections.sort(intPeerList, new Comparator<Integer>() {
	
							@Override
							public int compare(Integer o1, Integer o2) {
								Peer.PeerInfo peerInfo1 = currPeer.getOtherPeerInfo().get(o1);
								Peer.PeerInfo peerInfo2 = currPeer.getOtherPeerInfo().get(o2);

								if (peerInfo2.downloadRate > peerInfo1.downloadRate)
									return 1;
								if (peerInfo2.downloadRate < peerInfo1.downloadRate)
									return -1;
								return 0;
							}
						});
						System.out.println("Current peer DOES NOT haVE complete file: chossing peers BASED ON RATE, Peer list is: ");
						for(int j: intPeerList)
							System.out.print(j+",");
	
						sendChokeUnchoke(intPeerList, preferredNbrCount);
					}
				}
			}
			try {
			//	System.out.println("Sleeping for choke/unchoke interval");
				Thread.sleep(timer);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("Error in choking interval timer. Could not sleep");
				e.printStackTrace();
			}
		}
	}

	void sendChokeUnchoke(ArrayList<Integer> intPeerList, int preferredNbrCount) {

		ArrayList<Integer> selectedNbrs;
		if (preferredNbrCount < intPeerList.size())
			selectedNbrs = new ArrayList<Integer>(intPeerList.subList(0, preferredNbrCount));
		else
			selectedNbrs = intPeerList;

		boolean flag=true;
		
		ArrayList<Integer> myunchokedPeers = currPeer.getUnchokedPeers();
		for(int k:selectedNbrs)
		{
			boolean flag2=false;
			for(int l:myunchokedPeers)
			{
				if(k==l)
					flag2=true; // selectedNbr is in unchoked list
			}
			if(flag2==false)	// selectedNbr is not in unchoked list
				flag=false;
		}
		
		if(!flag){
			// need to print the log only on change
			String str = "";
			for(int sN : selectedNbrs)
			{
				str = str + sN + ", ";
			}
			str=str.substring(0,str.length()-2);

			P2PLogger.log("Peer "+currPeer.getPeerId()+" has preferred neighbors "+str+".",currPeer.getPeerId());
		}

		for (int i = 0; i < currPeer.getUnchokedPeers().size(); i++) {
			int peerIdtemp = currPeer.getUnchokedPeers().get(i);

			// TODO: check on optimistically unchocked peer
			if (!selectedNbrs.contains(peerIdtemp) && currPeer.getOptimiticallyUnchokedPeer() != peerIdtemp) {
				byte[] msg = Choke.getChokeMsg();
				System.out.println("Sending choke message to "+peerIdtemp);
				sendMessage(currPeer.getOtherPeerInfo().get(peerIdtemp).out, msg);
				currPeer.removeUnchokedPeer(peerIdtemp);
				System.out.println("Removing peer " + peerIdtemp + " from list of unchocked peers");
			}
		}

		for (int i = 0; i < selectedNbrs.size(); i++) {
			int peerIdtemp = selectedNbrs.get(i);
			if (!currPeer.getUnchokedPeers().contains(peerIdtemp) && currPeer.getOptimiticallyUnchokedPeer()!=peerIdtemp) {
				byte[] msg = Unchoke.getUnchokeMsg();
				System.out.println("Sending unchoke message to "+peerIdtemp);
				sendMessage(currPeer.getOtherPeerInfo().get(peerIdtemp).out, msg);
				System.out.println("Adding peer " + peerIdtemp + " to list of unchocked peers");
			}
				currPeer.addUnchokedPeer(peerIdtemp);
		}

	}

	void sendMessage(ObjectOutputStream out, byte[] msg) {
		synchronized (out) {
			
			try {
//				System.out.println("Sending message of length " + msg.length + " bytes");
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
