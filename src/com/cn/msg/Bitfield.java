package com.cn.msg;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Bitfield {

	private static final byte msgType = 32;

	public static byte[] getBitFieldMessage(boolean hasPieces[]){
		
		int msgLen = (int)Math.ceil(hasPieces.length/8.0);
		byte msg[]=new byte[(5+msgLen)];
		byte[] array = ByteBuffer.allocate(4).putInt(msgLen).array();
		int i;
		for(i=0;i<4;i++)
			msg[i]=array[i];
		msg[i++]=msgType;
		int z=7;
		for(int k=1;k<=hasPieces.length;k++)
		{
			if(hasPieces[k-1]){
				msg[i] |= (byte)Math.pow(2,z);
			}
			if(k%8==0)
			{
				i++;
				z=8;
			}
			z--;
		}
		return msg;
	}
	
	public static boolean[] getPiecesFromBitfield(byte msg[]){
		int len=msg.length;
		int payload_len = len-5;
		boolean pieces[] = new boolean[payload_len*8];
		int z=7;
		int k=5;
		for(int i=0;k<msg.length;i++){
			if((msg[k] & (byte)Math.pow(2, z)) != 0){
				pieces[i]=true;
			}
			z--;
			if(z<0){
				k++;
				z=7;
			}
		}
		return pieces;
		
	}
	/*
	public static void main(String ar[]){
		byte[] a = Bitfield.getBitFieldMessage(new byte[]{1,1,0,0,0,0,0,1,1,1,0,0,0,0,1,0});
		for(int i=128;i>=1;i/=2){
			System.out.print((a[5] & i) + "-");
		}
		System.out.println();
		for(int i=128;i>=1;i/=2){
			System.out.print((a[6] & i) + "-");
		}
		System.out.println(a);
		
		boolean pieces[]=Bitfield.getPiecesFromBitfield(a);
		System.out.println(pieces);
	}*/
}
