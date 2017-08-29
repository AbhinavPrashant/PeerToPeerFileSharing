package com.cn.handler;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.Set;

import com.cn.log.P2PLogger;
import com.cn.msg.Bitfield;
import com.cn.msg.Handshake;
import com.cn.msg.Have;
import com.cn.msg.Interested;
import com.cn.msg.NotInterested;
import com.cn.msg.Piece;
import com.cn.msg.Request;
import com.cn.peer.Peer;

public class MsgHandler extends Thread {

	Socket socket;
	Peer curPeer;
	int remotePeerId;
	ObjectInputStream in;
	ObjectOutputStream out;

	public MsgHandler(Socket socket, ObjectOutputStream out, Peer curPeer) {
		this.socket = socket;
		this.curPeer = curPeer;
		this.out = out;
	}

	public void run() {

		try {

			in = new ObjectInputStream(socket.getInputStream());
			// out= new ObjectOutputStream(socket.getOutputStream());
			while (true) {

				byte[] message;
				synchronized (in) {
					message = (byte[]) in.readObject();

				}
synchronized(curPeer){				
				if (Handshake.isHandshake(message)) {
					remotePeerId = Handshake.getRemotePeerId(message);
					if(remotePeerId>curPeer.getPeerId())
						P2PLogger.log("Peer "+curPeer.getPeerId()+" is connected from Peer "+remotePeerId+".",curPeer.getPeerId());
					System.out.println("Setting output stream for peerID " + remotePeerId);
					curPeer.setOutputStream(remotePeerId, out);
					System.out.println("Message is handshake message from peer " + remotePeerId);
					// check if curPeer has any pieces
					System.out.println("Check if curPeer has any pieces (is there a need to send Bitfiled) ");

					boolean pieces[] = curPeer.getPieces();
					boolean sendBitfield = false;
					for (int i = 0; i < pieces.length; i++) {
						if (pieces[i]) {
							sendBitfield = true;
							System.out.println("Cur Peer has some pieces, need to send BitField message");
							break;
						}
					}
					if (sendBitfield) {
						byte sendMsg[] = Bitfield.getBitFieldMessage(pieces);
						// TODO: log it
						System.out.println("Sending Bitfield message of length " + sendMsg.length);
						sendMessage(out, sendMsg);
					} else {
						System.out.println("Cur Peer has no pieces, no Bitfield message is sent");
					}

				} else {
					byte type = message[4];
					switch (type) {
					case 1: // choke
						System.out.println("Received choke message from peer "+remotePeerId);
						P2PLogger.log("Peer "+curPeer.getPeerId()+" is choked by "+remotePeerId+".",curPeer.getPeerId());
						curPeer.setUnchoked(false);
						curPeer.removeRequestedmessagesForPeer(remotePeerId);
						curPeer.getOtherPeerInfo().get(remotePeerId).setBytesSinceUnchoke(0);
						// TODO: verify this requirement
						// remove requested messages that have not been received from array
						break;
					case 2: // unchoke
						System.out.println("Received unchoke message from peer " + remotePeerId);
						P2PLogger.log("Peer "+curPeer.getPeerId()+" is unchoked by "+remotePeerId+".",curPeer.getPeerId());
						curPeer.setUnchoked(true);
						curPeer.startUnchokeCalculation(remotePeerId);
//						synchronized(curPeer){
							int pieceId = curPeer.getMissingPiece(remotePeerId);
							if (pieceId >= 0) {
								System.out.println("requesting piece " + pieceId + " from peer " + remotePeerId);
								// this piece has been requested, add to the already
								// requested piece array
								curPeer.addReqPiece(pieceId, remotePeerId);
								byte[] requestMsg = Request.getRequestMsg(pieceId);
								sendMessage(out, requestMsg);
							}
							else{
								System.out.println("Not requesting piece from peer "+remotePeerId+" since it doesnt have interesting pieces ");
							}
//						}	
						break;

					case 4: // interested - add to interested list
						System.out.println("Received interested message from peer " + remotePeerId);
						P2PLogger.log("Peer "+curPeer.getPeerId()+" received a ‘interested’ message from "+remotePeerId+".",curPeer.getPeerId());
						curPeer.addInterestedPeer(remotePeerId);
						break;
					case 8: // not interested - add to not interested list
						System.out.println("Received not interested message from peer " + remotePeerId);
						P2PLogger.log("Peer "+curPeer.getPeerId()+" received the ‘not interested’ message from "+remotePeerId+".",curPeer.getPeerId());
						curPeer.removeInterestedPeer(remotePeerId);
						break;
					case 16: // have
						int recvPieceIndex = Have.getPieceIndex(message);
						System.out.println("Received Have Message from remote peer " + remotePeerId + " for piece index "+ recvPieceIndex);
						P2PLogger.log("Peer "+curPeer.getPeerId()+" received the ‘have’ message from "+remotePeerId+" for the piece "+recvPieceIndex+".",curPeer.getPeerId());
//						synchronized(curPeer){
							
							curPeer.updateOtherPeerPiece(remotePeerId, recvPieceIndex);
							boolean checkFlag=false;
							boolean othersPieces[] = curPeer.getOtherPeerInfo().get(remotePeerId).getItsPieces();
							boolean ourPieces[] = curPeer.getPieces();
							for(int r=0;r<ourPieces.length;r++)
							{
								if(othersPieces[r] && !ourPieces[r]){
									checkFlag=true;
									break;
								}
							}

//							if (curPeer.getPieces()[recvPieceIndex]) {
							if(!checkFlag){
								byte[] notIntMsg = NotInterested.getNotInterestedMsg();
								System.out.println("Sending not interested message to " + remotePeerId);
								sendMessage(out, notIntMsg);
							} else {
								// send interested message
								byte[] intMsg = Interested.getInterestedMsg();
								System.out.println("Sending interested message to " + remotePeerId);
								sendMessage(out, intMsg);
							}
//						}
						break;
					case 32: // bitfield
						System.out.println("Received BitField message from peer " + remotePeerId);
						boolean remotePeerPieces[] = Bitfield.getPiecesFromBitfield(message);
				
//						synchronized(this){
//							if(remotePeerId==1001)
//							{	System.out.println("Bitfield for peer 1001 is");
//								for(int z=0;z<remotePeerPieces.length;z++)
//									System.out.print(remotePeerPieces[z]);	
//								System.out.println();
//							}
//						}
						System.out.println("Updating remotePeers pieces ");
						for (int i = 0; i < remotePeerPieces.length; i++)
							if (remotePeerPieces[i])
								curPeer.updateOtherPeerPiece(remotePeerId, i);

//						synchronized(this){
//							HashMap<Integer,Peer.PeerInfo> myMap = curPeer.getOtherPeerInfo();
//							if(remotePeerId==1001)
//							{
//								System.out.println("Updated hasPieces for peer 1001 is");
//								Peer.PeerInfo peer1001 = myMap.get(1001);
//								for(int z=0;z<peer1001.hasPieces.length;z++){
//									System.out.print(peer1001.hasPieces[z]);
//								}
//								System.out.println();
//							}
//						}
						System.out.println("Checking if remotePeer" + remotePeerId + " has an interesting piece");
						// TODO: the following function must take care of
						// requested piecces as well
						if (curPeer.getMissingPiece(remotePeerId) != -1) // remotePeerID
																			// has
																			// an
																			// interesting
																			// piece
						{
							System.out.println("Remote peer " + remotePeerId + " has interesting pieces");
							System.out.println("Need to send intested message");
							byte sendMsg[] = Interested.getInterestedMsg();
							System.out.println("Sending interested message to peer "+remotePeerId);
							sendMessage(out, sendMsg);
						} else {
							System.out.println("remote peer " + remotePeerId + " does not have any intesting pieces");
							System.out.println("Need to send not interested message");
							byte sendMsg[] = NotInterested.getNotInterestedMsg();
							System.out.println("Sending not interested message to peer "+remotePeerId);
							sendMessage(out, sendMsg);
						}
						// update peers
						break;
					case 64: // request
						int requestedIndex = (Request.getPieceIndex(message));
						System.out.println("Received request message from peer "+remotePeerId+" for piece "+requestedIndex);
						byte[] pieceContent = curPeer.getPieceContent(requestedIndex);
						byte[] sndMsg =  Piece.getPieceMesage(requestedIndex, pieceContent);
						System.out.println("Sending piece message for piece "+requestedIndex+ " to peer" + remotePeerId);
						sendMessage(out, sndMsg);

						break;
					case -127: // piece // TODO: check -127
						// TODO: set msgType in piece to -127
						// TODO: update curPeer hasPieces, it is used during
						// BitField
						int pieceIndex = Piece.getPieceIndex(message);
						System.out.println("Piece message received from peer "+ remotePeerId+ " for pieceIdx-"+pieceIndex);

						byte[] pieces = Piece.getPieces(message);
						curPeer.addBytestoCalculation(remotePeerId, pieces.length);
						curPeer.setFilePieceAtIndex(pieceIndex, pieces);
						P2PLogger.log("Peer "+curPeer.getPeerId()+" has downloaded the piece "+pieceIndex+" from "+remotePeerId+". Now the number of pieces it has is "+curPeer.getCurPieces(),curPeer.getPeerId());
						// update hasPieces

//						synchronized(this){				
							boolean checkFlag1=false;
							boolean othersPieces1[] = curPeer.getOtherPeerInfo().get(remotePeerId).getItsPieces();
							boolean ourPieces1[] = curPeer.getPieces();
							for(int r=0;r<ourPieces1.length;r++)
							{
								if(othersPieces1[r] && !ourPieces1[r]){
									checkFlag1=true;
									break;
								}
							}

//							if (curPeer.getPieces()[recvPieceIndex]) {
							if(!checkFlag1){
								byte[] notIntMsg1 = NotInterested.getNotInterestedMsg();
								System.out.println("Sending not interested message to " + remotePeerId);
								sendMessage(out, notIntMsg1);
							} //else {
								// send interested message
							//	byte[] intMsg2 = Interested.getInterestedMsg();
							//	System.out.println("Sending interested message to " + remotePeerId);
							//	sendMessage(out, intMsg2);
							//}

//						}

						boolean[] hasPieces = curPeer.getPieces();
					//	hasPieces[pieceIndex] = true;
						// update has File, if all pieces are set
						curPeer.setHasFile(fileDownloadCompleted(hasPieces));
						curPeer.removeReqPiece(pieceIndex, remotePeerId);
						byte[] haveMsg = Have.getHaveMsg(pieceIndex);
						Set<Entry<Integer, Peer.PeerInfo>> entrySet = curPeer.getOtherPeerInfo().entrySet();
						for (Entry<Integer, Peer.PeerInfo> entry : entrySet) {
							int peerId = entry.getKey();
							if (peerId != curPeer.getPeerId()) {
								Peer.PeerInfo peerInfo = entry.getValue();
								if(peerInfo.out!=null){
									System.out.println("Sending have message to peer "+peerId);
									sendMessage(peerInfo.out, haveMsg);
								}
							}
						}

						if (curPeer.isUnchoked() && !curPeer.isHasFile()) {
//							synchronized(curPeer){
								int pieceIdx = curPeer.getMissingPiece(remotePeerId);
								System.out.println("Get missing piece gave missing piece as "+pieceIdx);
								if(pieceIdx >= 0){ 
									System.out.println("requesting piece " + pieceIdx + " from peer "+remotePeerId);
									// this piece has been requested, add to the already
									// requested piece array
									curPeer.addReqPiece(pieceIdx, remotePeerId);
									byte[] requestMsg = Request.getRequestMsg(pieceIdx);
									System.out.println("SEnding request message to peer "+remotePeerId);
									sendMessage(out, requestMsg);
								}
							
//							}
						}
						break;
					default:
						System.out.println("ERROR unknown message: received bytes are :");
						for(int m=0;m<message.length;m++)
						{
							System.out.print(message[m]+"\t");
						}
						System.out.println();
						break;
					}
				}
}
			}
		}
		catch(EOFException e){		
			System.out.println("Remote connection closed!");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	void sendMessage(ObjectOutputStream out, byte[] msg) {
		synchronized (out) {
			try {
				//System.out.println("Sending message of length " + msg.length + " bytes");
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

	private boolean fileDownloadCompleted(boolean[] hasPieces) {
	synchronized(curPeer){
		for (boolean hasPiece : hasPieces) {
			if (!hasPiece)
				return false;
		}
		System.out.println("Complete file downloaded, now creating file on disk");
		// now create file on the disk
		try {
			curPeer.createFile();
			System.out.println("file should have been created on disk");
		} catch (IOException e) {
			P2PLogger.log(e.getMessage(),curPeer.getPeerId());
		}
		return true;
	}
	}
}
