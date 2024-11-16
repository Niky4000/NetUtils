package com.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;

/**
 *
 * @author me
 */
public class Encrypt {

	private static final Integer BUFFER_LENGTH = 100;
	private static final String PRIVATE_KEY = "private.key";
	private static final String PUBLIC_KEY = "public.key";
	private static final String SIGNATURE = "signature.key";
	private static final String algorithm = "RSA"; // or RSA, DH, etc.

	private static KeyPair loadKeyPair(byte[] privateKeyBytes, byte[] publicKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
		EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
		PrivateKey privateKey2 = keyFactory.generatePrivate(privateKeySpec);
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
		PublicKey publicKey2 = keyFactory.generatePublic(publicKeySpec);
		return new KeyPair(publicKey2, privateKey2);
	}

	public static KeyPair loadKeysFromResources(Class class_) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] publicKeyBytes = getKeyBytes(class_, PUBLIC_KEY);
		byte[] privateKeyBytes = getKeyBytes(class_, PRIVATE_KEY);
		return loadKeyPair(privateKeyBytes, publicKeyBytes);
	}

	private static byte[] getKeyBytes(Class class_, String keyName) throws IOException {
		byte[] buffer = new byte[4096];
		InputStream resourceAsStream = class_.getClassLoader().getResourceAsStream(keyName);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		int read = 0;
		do {
			read = resourceAsStream.read(buffer);
			if (read > 0) {
				byteArrayOutputStream.write(buffer, 0, read);
			}
		} while (read >= 0);
		return byteArrayOutputStream.toByteArray();
	}

	public static List<byte[]> crypt(Key k, String data) throws Exception {
		byte[] buffer = new byte[BUFFER_LENGTH];
		int read = 0;
		List<byte[]> byteList = new ArrayList<>();
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data.getBytes());
		do {
			read = byteInputStream.read(buffer);
			if (BUFFER_LENGTH.equals(read)) {
				System.out.print(new String(buffer));
				byteList.add(handleData(k, buffer, true));
			} else if (read > 0) {
				byte[] smallBuffer = new byte[read];
				System.arraycopy(buffer, 0, smallBuffer, 0, read);
				System.out.print(new String(smallBuffer));
				byteList.add(handleData(k, smallBuffer, true));
			}
		} while (read > 0);
		return byteList;
	}

	public static String decrypt(Key k, List<byte[]> data) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (byte[] b : data) {
			sb.append(new String(handleData(k, b, false)));
		}
		return sb.toString();
	}

	private static byte[] handleData(Key k, byte[] data, boolean encrypt) throws Exception {
		if (k != null) {
			Cipher cipher = Cipher.getInstance("RSA");
			if (encrypt) {
				cipher.init(Cipher.ENCRYPT_MODE, k);
				byte[] resultBytes = cipher.doFinal(data);
				return resultBytes;
			} else {
				cipher.init(Cipher.DECRYPT_MODE, k);
				byte[] resultBytes = cipher.doFinal(data);
				return resultBytes;
			}
		}
		return null;
	}
}
