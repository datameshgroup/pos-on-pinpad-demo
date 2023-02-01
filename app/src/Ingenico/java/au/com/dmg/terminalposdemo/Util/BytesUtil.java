package au.com.dmg.terminalposdemo.Util;

public class BytesUtil {
	private static final String EMPTY_STRING = "";
	private BytesUtil() {}

	public static String bytes2HexString(byte[] data) {
		if (isNullEmpty(data)) {
			return EMPTY_STRING;
		}
		StringBuilder buffer = new StringBuilder();
		for (byte b : data) {
			String hex = Integer.toHexString(b & 0xff);
			if (hex.length() == 1) {
				buffer.append('0');
			}
			buffer.append(hex);
		}
		return buffer.toString().toUpperCase();
	}

	public static boolean isNullEmpty(byte[] array) {
		return (array == null) || (array.length == 0);
	}

}
