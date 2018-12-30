package org.csc.browserAPI.util;

import java.math.BigInteger;
import java.text.DecimalFormat;

import org.csc.bcapi.EncAPI;

import com.google.protobuf.ByteString;

public class DataUtil {

	public static String byteString2String(ByteString src, EncAPI encApi){
		if(src != null){
			return encApi.hexEnc(src.toByteArray());
		}
			
		return "";
	}
	
	public static String formateStr(String source) {
		String target = "";
		target = new DecimalFormat("#0.00").format(Double.valueOf(source));
//		if (Double.valueOf(source) >= 1)
//		else {
//			target = "0" + new DecimalFormat("#.00").format(Double.valueOf(source));
//		}

		return target;
	}
	
	public static BigInteger toNormal(BigInteger val) {
		return val.divide(new BigInteger("1000000000000000000"));
	}
}
