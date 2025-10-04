package com.coniv.mait.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Base62Convertor {

	private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

	public static String uuidToBase62(UUID uuid) {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
		buffer.putLong(uuid.getMostSignificantBits());
		buffer.putLong(uuid.getLeastSignificantBits());
		BigInteger bigInt = new BigInteger(1, buffer.array());

		StringBuilder sb = new StringBuilder();
		while (bigInt.compareTo(BigInteger.ZERO) > 0) {
			BigInteger[] divRem = bigInt.divideAndRemainder(BigInteger.valueOf(62));
			sb.append(BASE62[divRem[1].intValue()]);
			bigInt = divRem[0];
		}

		return sb.reverse().toString();
	}

}
