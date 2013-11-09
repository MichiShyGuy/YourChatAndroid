package de.deyovi.yourchatandroid.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

public class Authenticator {

	private final static String TAG = Authenticator.class.getName();
	
	private final static String ALGORITHM = "SHA-256";
	private static MessageDigest md;
	
	public synchronized static String encrypt(String pwhash, String sugar) {
		String result = null;
		if (pwhash != null) {
			try {
				if (md == null) {
					md = MessageDigest.getInstance(ALGORITHM);
				} else {
					md.reset();
				}
				if (sugar != null) {
					md.update(sugar.getBytes("UTF-8"));
				}
				md.update(pwhash.getBytes("UTF-8"));
				byte[] digest = md.digest();
				result = bytesToHex(digest);
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "Error while creating a MessageDigester for algorithm: " + ALGORITHM, e);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "Error while reading Bytes", e);
			} finally {
				if (md != null) {
					md.reset();
				}
			}
		}
		return result;
	}

	private static String bytesToHex(byte[] bytes) {
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static interface AuthenticationCallback {
		
		public void success(String listenId);
		
		public void failure(String reason);
		
	}
}
