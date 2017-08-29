package com.cn.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import com.cn.peer.Peer;
import com.cn.res.IConstants;
import com.cn.res.InvalidInputFormatException;

public class PeerInfoReader {

	private String fileName;
	private LinkedHashMap<Integer,Peer> peerMap;


	public PeerInfoReader(String fileName) throws Exception {
		super();
		this.fileName = fileName;
		this.peerMap = new LinkedHashMap<Integer,Peer>();
		init();
		
	}

	private void init() throws IOException, ParseException, InvalidInputFormatException {
		 File file = new File(this.fileName);
		 BufferedReader reader = new BufferedReader(new FileReader(file));
		 read(reader);
	}

	public LinkedList<Peer> getPeerList() {
		return new LinkedList<Peer>(peerMap.values());
	}

	public Peer getPeerFromPeerId(int curPeerId) {
		return peerMap.get(curPeerId);
	}
	
	
	 private void read (Reader reader) throws FileNotFoundException, IOException, ParseException, InvalidInputFormatException {
	        BufferedReader in = new BufferedReader(reader);
	        String line  = "";
	        while ((line = in.readLine()) != null) {
	            line = line.trim();
	            if ((line.length() <= 0) || (line.startsWith (IConstants.COMMENT_CHAR))) {
	                continue;
	            }
	            
	            Peer peer = createPeerFromString(line);
	            peerMap.put(peer.getPeerId(), peer);
	        }
	        for(int p:peerMap.keySet()){
	        	Peer z = peerMap.get(p);
	        	z.initializeOtherPeerInfo(peerMap);
	        }
	    }
	 
	 /**
	 * creates Peer object from 1 line string 
	 * @param line
	 * @return
	 * @throws InvalidInputFormatException
	 * @throws IOException 
	 */
	private Peer createPeerFromString(String line) throws InvalidInputFormatException, IOException{
		 
		 String[] tokens = line.split("\\s+");
         if (tokens.length != 4) {
             throw new InvalidInputFormatException(line);
         } 
         
         int peerID = Integer.parseInt(tokens[0]);
         String peerAdd = tokens[1];
         int peerPort = Integer.parseInt(tokens[2]);
         System.out.println("Boolean.getBoolean(tokens[3]) " + Boolean.getBoolean(tokens[3])+ "\n" + "tokens[3] " + tokens[3]);
         boolean  hasFile = Integer.parseInt(tokens[3]) ==1;
         
         
		 return new Peer(peerID,peerAdd,peerPort,hasFile,peerMap);
	 }

}

