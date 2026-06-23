package android_serialport_api;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

/**
 * 数据转换工具
 */
@SuppressLint("DefaultLocale")
public class MyFunc {

	static public void delayMs(int time)
	{
		Handler handler=new Handler();
		Runnable runnable=new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
			}
		};

		handler.postDelayed(runnable, time);
	}

	// -------------------------------------------------------
	// 判断奇数或偶数，位运算，最后一位是1则为奇数，为0是偶数
	static public int isOdd(int num) {
		return num & 0x1;
	}

	// 异或运算
	static public byte bccCalc(byte[] data)
	{
		byte bccresult=0;

		for(int i=0; i< data.length; i++)
		{
			bccresult ^= data[i];
		}

		return bccresult;
	}

	// 异或运算
	static public byte bccCalc(byte[] data, int offset, int byteCount)
	{
		byte bccresult=0;

		for(int i=offset; i< offset+byteCount; i++)
		{
			bccresult ^= data[i];
		}

		return bccresult;
	}

	static public void reverseByte(byte[] bytes){
		int i;
		byte temp;

		for(i=0; i<bytes.length/2; i++)
		{
			temp = bytes[i];
			bytes[i] = bytes[bytes.length-i-1];
			bytes[bytes.length-i-1] = temp;
		}
	}

	static public long bytetoInt(byte[] bytes){
		long number = 0;
		for(int i = 0; i < bytes.length ; i++) {
//			number += (long)(bytes[i] << i*8);

			number <<= 8;
			number += bytes[i];
		}

		return number;
	}

	static public long bytetoInt(byte[] bytes, int offset, int byteCount){

		long number = 0;
		for(int i = offset; i < offset+byteCount; i++) {
//			number += (long)(bytes[i] << i*8);

			number += (long)(bytes[i]<<(offset+byteCount-i-1)*8);
			Log.d("recv long:",""+number);
		}

		return number;
	}

	// -------------------------------------------------------
	static public int HexToInt(String inHex)// Hex字符串转int
	{
		return Integer.parseInt(inHex, 16);
	}

	// -------------------------------------------------------
	static public byte HexToByte(String inHex)// Hex字符串转byte
	{
		return (byte) Integer.parseInt(inHex, 16);
	}

	// -------------------------------------------------------
	static public String Byte2Hex(Byte inByte)// 1字节转2个Hex字符
	{
		return String.format("%02x", inByte).toUpperCase();
	}

	// -------------------------------------------------------
	static public String ByteArrToHex(byte[] inBytArr)// 字节数组转转hex字符串
	{
		StringBuilder strBuilder = new StringBuilder();
		int j = inBytArr.length;
		for (int i = 0; i < j; i++) {
			strBuilder.append(Byte2Hex(inBytArr[i]));
			strBuilder.append(" ");
		}
		return strBuilder.toString();
	}

	// -------------------------------------------------------
	static public String ByteArrToHex(byte[] inBytArr, int offset, int byteCount)// 字节数组转转hex字符串，可选长度
	{
		StringBuilder strBuilder = new StringBuilder();
		int j = byteCount;
		for (int i = offset; i < offset+j; i++) {
			strBuilder.append(Byte2Hex(inBytArr[i]));
		}
		return strBuilder.toString();
	}

	// -------------------------------------------------------
	// 转hex字符串转字节数组
	static public byte[] HexToByteArr(String inHex)// hex字符串转字节数组
	{
		int hexlen = inHex.length();
		byte[] result;
		if (isOdd(hexlen) == 1) {// 奇数
			hexlen++;
			result = new byte[(hexlen / 2)];
			inHex = "0" + inHex;
		} else {// 偶数
			result = new byte[(hexlen / 2)];
		}
		int j = 0;
		for (int i = 0; i < hexlen; i += 2) {
			result[j] = HexToByte(inHex.substring(i, i + 2));
			j++;
		}
		return result;
	}
}