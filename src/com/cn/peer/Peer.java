package com.cn.peer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.cn.config.CommonConfigEnum;
import com.cn.config.CommonConfigReader;
import com.cn.log.P2PLogger;

public class Peer {
	private int peerId;
	private String peerAddr;
	private int peerPort;
	private boolean hasFile;
	private boolean[] hasPieces;
	private List<Socket> sockets;
	private HashMap<Integer, PeerInfo> otherPeerInfo;
	private CommonConfigReader properties;
	private File myFile;
	private int curPieces;

	public int getCurPieces(){	
		synchronized(this){	
			return curPieces;
		}
	}
	
	private boolean isUnchoked = false;

	public boolean isUnchoked() {
		synchronized(this){
			return isUnchoked;
		}
	}

	public void setUnchoked(boolean isUnchoked) {
		synchronized(this){
			this.isUnchoked = isUnchoked;
		}
	}
	
	

	private int numPieces;
	private long fileSize;
	private int pieceSize;
	private ArrayList<Integer> interestedPeers = new ArrayList<>();;
	private ArrayList<Integer> unchokedPeers = new ArrayList<>();;
	private int optimiticallyUnchokedPeer = -1;
	private HashMap<Integer,Integer> alreadyReq = new HashMap<>(); // Piece/peer
	
	class File{
		Piece[] pieces; 
		
		class Piece{
			byte[] arr;
			public Piece(byte[] arr) {
				this.arr = Arrays.copyOf(arr,arr.length);
			}
		
			
		}
		public File(CommonConfigReader reader, boolean hasFile) {
			long fileSize = Integer.parseInt(properties.get(CommonConfigEnum.FileSize.toString()).toString());
			int pieceSize = Integer.parseInt(properties.get(CommonConfigEnum.PieceSize.toString()).toString());
			int nPieces=(int) Math.ceil((double)fileSize/(double)pieceSize);
			
			pieces = new Piece[nPieces];
			if(hasFile){
				String fName = properties.get(CommonConfigEnum.FileName.toString()).toString();
				try {
					byte[] data = Files.readAllBytes(Paths.get(fName));
					int i = 0;
					int k=0;
					while(i<data.length){
						byte thisPiece[]=Arrays.copyOfRange(data, i, i+pieceSize);
						i=i+pieceSize;
						pieces[k++]=new Piece(thisPiece);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
			else{
				byte arr[] = new byte[pieceSize];
				for(int i=0;i<nPieces;i++)
				{
					pieces[i] = new Piece(arr);
				}
			}
			
		}
		
		public void setFilePieceAtIndex(int ind,byte[] content){
			synchronized(this){
				pieces[ind] = new Piece(content);
				hasPieces[ind]=true;
			}
		}
		public byte[] getPiece(int index){
			synchronized(this){
				return pieces[index].arr;
			}
		}
		
	}
	public boolean haveAllOtherFilesDownloaded(){
		synchronized(this){
			for(int peerId: otherPeerInfo.keySet()){
				if(peerId == this.peerId && this.hasFile)
					continue;
				Peer.PeerInfo pInfo = otherPeerInfo.get(peerId);
				boolean pcs[] = pInfo.hasPieces; 
				for(int z=0;z<pcs.length;z++){
					if(!pcs[z]){
						System.out.println("Pieces for peerId" + peerId + " not complete yet ");
						return false;
					}
				}
			}
			return true;
		}
	}

	public void removeRequestedmessagesForPeer(int peerId){
		synchronized(this){
			Set<Integer> keys = new HashSet<>(alreadyReq.keySet());
			for(int i: keys){
				if(alreadyReq.get(i) == peerId)
					alreadyReq.remove(i);
			}
		}
	}	
	public void setFilePieceAtIndex(int index,byte[] piece){
		synchronized(this){
			this.myFile.setFilePieceAtIndex(index, piece);
			curPieces++;
		}
	}
	public class PeerInfo{
		boolean hasPieces[];
		public double downloadRate;
		public ObjectOutputStream out;
		long unchokeStartTime;
		long bytesSinceUnchoke;
		public PeerInfo(int pieces){
			hasPieces =  new boolean[pieces];
			downloadRate = 0.0;
			unchokeStartTime = Calendar.getInstance().getTimeInMillis();
		}
		public void setOutputStream(ObjectOutputStream out){
			this.out = out;
			System.out.println("Output stream set");
		}
		public void setUnchokeTimeBytes(){
			unchokeStartTime = Calendar.getInstance().getTimeInMillis();
			bytesSinceUnchoke=0;
		}
		public void addBytes(int bytes){
			bytesSinceUnchoke+=bytes;
			System.out.println("Bytes received by this peer is now "+bytesSinceUnchoke);
		}
		public long getUnchokeStartTime(){
			return unchokeStartTime;
		}
		public long getBytesSinceUnchoke(){
			return bytesSinceUnchoke;
		}
		public void setDownloadRate(double calculatedDownloadRate) {
			downloadRate = calculatedDownloadRate;
	
		}
		public boolean[] getItsPieces(){	
			return hasPieces;
		}
		public void setBytesSinceUnchoke(int bytes){	
			bytesSinceUnchoke = bytes;
		}
	}

	public void startUnchokeCalculation(int peerId){
		System.out.println("Resetting unchokeTime and Bytes for peerId  "+peerId);
		otherPeerInfo.get(peerId).setUnchokeTimeBytes();
	}
	public void addBytestoCalculation(int peerId,int bytes){
		System.out.println("Adding bytes received to peer "+peerId);
		otherPeerInfo.get(peerId).addBytes(bytes);
	}

	public byte[] getPieceContent(int index){
		synchronized(this){
			return myFile.getPiece(index);
		}
	}
	// rishabh- check this
	public HashMap<Integer, PeerInfo> getOtherPeerInfo() {
		synchronized(this){
			return otherPeerInfo;
		}
	}

	public void setOutputStream(int peerId,ObjectOutputStream out){
		synchronized(this){
			otherPeerInfo.get(peerId).setOutputStream(out);
		}
	}
	

	public Peer(int peerId, String peerAddr, int peerPort, boolean hasFile, LinkedHashMap<Integer,Peer> peerList) throws IOException {
		super();
		this.peerId = peerId;
		this.peerAddr = peerAddr;
		this.peerPort = peerPort;
		this.hasFile = hasFile;
		
		properties = new CommonConfigReader();
		properties.loadProperties();
		long fileSize = Long.parseLong(properties.get(CommonConfigEnum.FileSize.toString()).toString());
		int pieceSize = Integer.parseInt(properties.get(CommonConfigEnum.PieceSize.toString()).toString());
		int pieces=(int) Math.ceil((double)fileSize/pieceSize);
		this.hasPieces = new boolean[pieces];
		this.fileSize = fileSize;
		this.pieceSize = pieceSize;
		numPieces = pieces;
		// if hasfile is true then peer has complete file so filling hasPices with 1, 0 otherwise
		Arrays.fill(hasPieces, hasFile?true:false);
		sockets = new ArrayList<Socket>();
		otherPeerInfo = new HashMap<>();
		myFile = new File(properties, hasFile);
	}
	public CommonConfigReader getProperties() {
		return properties;
	}

	public void updateOtherPeerPiece(int peerID, int pieceIndex){
		synchronized(this){
			if(pieceIndex < numPieces)
				otherPeerInfo.get(peerID).hasPieces[pieceIndex]=true;
		}
	}
	
	
	public void addSocket(Socket s){
		synchronized(this){
			sockets.add(s);
		}
	}

	public int getPeerId() {
		return peerId;
	}

	public String getPeerAddr() {
		return peerAddr;
	}
	
	public int getPeerPort() {
		return peerPort;
	}

	public String getPeerString() {
		return "PeerID: "+peerId+", HostName: "+peerAddr +", Port: "+peerPort;
	}

	public boolean isHasFile() {
		synchronized(this){
			return hasFile;
		}
	}
	
	public void setHasFile(boolean hasFile){
		synchronized(this){
			this.hasFile = hasFile;
		}
	}
	/*public void setPieces(byte [] pieces){
		System.out.println("Settung pieces for peer "+peerId);
		System.arraycopy(pieces, 0, hasPieces, 0, pieces.length);
		System.out.println("Updated pieces for peer "+peerId+": "+Arrays.toString(hasPieces));
	}*/
	
	public boolean[] getPieces (){
		synchronized(this){
			return hasPieces;
		}
	}
	
	/*
	// Checks if the pieces are currently available on the peer.
	// To be used when "bitfield" message arrives.
	//
	public boolean pieceRequired (byte [] remotePieces){
		for (int i =0; i< remotePieces.length; i++){
			byte tempHasPiece = hasPieces[i];
			byte tempRemotePiece = remotePieces[i];	
			for (int j =7; j >=0;j--){
				if (((tempHasPiece & (1 << j)) == 0) &&
					((tempRemotePiece & (1 << j))== 1)){
					return true;
				}
			}
		}
		return false;
	}
	
	// Checks if the pieces are currently available on the peer.
	//  To be used when "Have" message arrives.
	 
	public boolean pieceRequired (int pieceIndex){
		byte temp = hasPieces[pieceIndex/8];
		
		if ((temp & (1 << 7-(pieceIndex % 8)))==0) 
			return true;
		else
			return false;
	}
	*/
	public void initializeOtherPeerInfo(LinkedHashMap<Integer, Peer> peerMap) {
		for(int id:peerMap.keySet()){
			otherPeerInfo.put(id, new PeerInfo(numPieces));
		}
	}

	public ArrayList<Integer> getInterestedPeers() {
		synchronized(this){
			return interestedPeers;
		}
	}

	public void addInterestedPeer(int peerId) {
		synchronized(this){
			if(interestedPeers.indexOf(peerId)==-1)
				interestedPeers.add(peerId);
		}
	}
	public void removeInterestedPeer(int peerId){
		synchronized(this){
			int index=interestedPeers.indexOf(peerId);
			if(index!=-1)
				interestedPeers.remove(index);
		}
	}

	public ArrayList<Integer> getUnchokedPeers() {
		synchronized(this){
			return unchokedPeers;
		}
	}

	public void addUnchokedPeer(int peerId) {
		synchronized(this)
		{
			if(unchokedPeers.indexOf(peerId)==-1)
				unchokedPeers.add(peerId);
		}
	}
	
	public void removeUnchokedPeer(int peerId) {
		synchronized(this){
			int index = unchokedPeers.indexOf(peerId);
			if(index!=-1)
				unchokedPeers.remove(index);
		}
	}
	
	public int getOptimiticallyUnchokedPeer() {
		synchronized(this){
			return optimiticallyUnchokedPeer;
		}
	}

	public void setOptimiticallyUnchokedPeer(int optimiticallyUnchokedPeer) {
		synchronized(this){
			this.optimiticallyUnchokedPeer = optimiticallyUnchokedPeer;
		}
	}

	/**
	 * 
	 * @param remotePeerId
	 * @return missing piece index
	 */
	public int getMissingPiece(int remotePeerId) {
		
		synchronized(this){
			boolean[] currPieces = this.getPieces();
			PeerInfo peerInfo = this.getOtherPeerInfo().get(remotePeerId);
			int ind = 0;
			boolean[] remoteHasPieces = peerInfo.hasPieces;
			List<Integer> missingPieces = new ArrayList<>();	
			
			for(int m=0;m<currPieces.length;m++)
			{
				if(!currPieces[m] && remoteHasPieces[m] && !alreadyReq.containsKey(m))
					missingPieces.add(m);
			}
		//	(boolean cp:currPieces){
		//		for(boolean rp:remoteHasPieces){
		//			if(!cp && !alreadyReq.containsKey(ind) && rp ){
		//				missingPieces.add(ind);
		//			}
		//		}
		//		ind++;
		//		
		//	}
			if(missingPieces.size() == 0) return -1;
			
			Random random = new Random();
			int randInd = random.nextInt(missingPieces.size());
			
			return missingPieces.get(randInd);
		}
	}
	public void addReqPiece(int requestedPieceInd,int requestedPeerId){
		synchronized(this){
			alreadyReq.put(requestedPieceInd,requestedPeerId);
		}
	}
	public void removeReqPiece(int requestedPieceInd,int requestedPeerId){
		synchronized(this){
			alreadyReq.remove(requestedPieceInd);
		}
	}
	
	public void createFile() throws IOException{
	synchronized(this){
		String fName = properties.get(CommonConfigEnum.FileName.toString()).toString();
		java.io.File folder = new java.io.File("Peer_"+getPeerId());
		folder.mkdir();
		java.io.File file = new java.io.File(folder+"/"+fName);
		
	//	fName = peerId + "_"+fName;
	//	System.out.println("Creating file with name "+fName);
		File.Piece[] pieces = this.myFile.pieces;
		FileOutputStream fos = new FileOutputStream(file);
		int thispiece=0;
		for (File.Piece piece : pieces) {
			if(thispiece == numPieces-1)
			{
				int numBytesLastPiece=(int)(fileSize%(long)pieceSize);
				if(numBytesLastPiece==0)
					numBytesLastPiece = pieceSize;
				fos.write(Arrays.copyOfRange(piece.arr,0,numBytesLastPiece));
			}
			else{
				fos.write(piece.arr);			
			}
			thispiece++;
		
		}
		P2PLogger.log("Peer "+this.peerId+" has downloaded the complete file.",this.peerId);		
		fos.close();
	}
	}
}
