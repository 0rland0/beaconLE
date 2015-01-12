package edu.teco.bpart;

public class MathHelper {

	 // Simple byte-to-hex-string thing from Stackoverflow.
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
