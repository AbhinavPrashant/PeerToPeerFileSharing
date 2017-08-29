package com.cn.msg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


public class Piece {

	
	public static byte[] getPieceMesage(int pieceIndex, byte[] content){
		int msgLen = 1 + 4 + content.length; // 1 for msgType, 4 for pieceIndex and rest for piece itself
		byte msgType = -127;
		byte msg[]=new byte[(4+msgLen)];
		byte[] array = ByteBuffer.allocate(4).putInt(msgLen).array();
		int i;
		for(i=0;i<4;i++)
			msg[i]=array[i];
		
		msg[i++]=msgType;
		byte[] arrayPieceInd = ByteBuffer.allocate(4).putInt(pieceIndex).array();
		for(;i<9;i++)
			msg[i]=arrayPieceInd[i-arrayPieceInd.length-1];
		int k=0;
		for(;k<content.length;k++){
			msg[i+k] = content[k];
		}
		return msg;
	}
	
	public static int getPieceIndex(byte msg[]) {
		return ByteBuffer.wrap(Arrays.copyOfRange(msg, 5, 9)).order(ByteOrder.BIG_ENDIAN).getInt();
	}
	
	public static byte[] getPieces(byte msg[]){
		return Arrays.copyOfRange(msg, 9, msg.length);
	}
	
	/* public Piece (int pieceIdx, byte[] content) {
	        super (Type.Piece, join (pieceIdx, content));
	    }

	    public byte[] getContent() {
	        if ((_payload == null) || (_payload.length <= 4)) {
	            return null;
	        }
	        return Arrays.copyOfRange(_payload, 4, _payload.length);
	    }

	    private static byte[] join (int pieceIdx, byte[] second) { 
	        byte[] concat = new byte[4 + (second == null ? 0 : second.length)];
	        System.arraycopy(getPieceIndexBytes (pieceIdx), 0, concat, 0, 4);
	        System.arraycopy(second, 0, concat, 4, second.length);
	        return concat;
	    }
*/
}
