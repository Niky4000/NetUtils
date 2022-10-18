package com.lib;

import static com.crypto.Encrypt.crypt;
import static com.crypto.Encrypt.decrypt;
import static com.crypto.Encrypt.loadKeysFromResources;
import static com.utils.Utils.getPathToSelfJar;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ConfigHandler {

	private static final String SPLITTER = "@";
	private final String saveFolderName;
	private final String cryptedConfigName;
	private static final int CRYPTO_CHUNK_LENGTH = 128;

	public ConfigHandler(String saveFolderName, String cryptedConfigName) {
		this.saveFolderName = saveFolderName;
		this.cryptedConfigName = cryptedConfigName;
	}

	public void start(String[] args, BiConsumer<String[], List<List<String>>> argumentsHandler) throws Exception {
		if (args.length > 0) {
			if (args[0].equals("CRYPT")) {
				List<String> plainArgList = new ArrayList(Arrays.asList(args));
				plainArgList.remove(0);
				if (!plainArgList.isEmpty()) {
					cryptConfigs(plainArgList);
				} else {
					System.out.println("There is no arguments!");
				}
			} else if (args[0].equals("SHOW")) {
				System.out.println(getDecrypted());
			} else {
				List<List<String>> argList2 = getArgList(args);
				argumentsHandler.accept(args, argList2);
			}
		} else {
			String decrypted = getDecrypted();
			String[] decryptedArgs = decrypted.split(" ");
			List<List<String>> argList2 = getArgList(decryptedArgs);
			argumentsHandler.accept(decryptedArgs, argList2);
		}
	}

	public void cryptConfigs(List<String> plainArgList) throws Exception, InvalidKeySpecException, IOException, NoSuchAlgorithmException {
		File pathToSelfJar = getPathToSelfJar();
		KeyPair keyPair = loadKeysFromResources(ConfigHandler.class);
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		String data = plainArgList.stream().reduce((str1, str2) -> str1 + " " + str2).get();
		List<byte[]> crypted = crypt(publicKey, data);
		String decrypted = decrypt(privateKey, crypted);
		File saveFolder = new File(pathToSelfJar.getParentFile().getAbsolutePath() + File.separator + saveFolderName);
		File cryptedConfig = new File(saveFolder.getAbsolutePath() + File.separator + cryptedConfigName);
		saveToFile(cryptedConfig, crypted);
	}

	private void saveToFile(File file, List<byte[]> dataList) throws IOException {
		if (file.exists()) {
			file.delete();
		}
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		file.createNewFile();
		try (OutputStream outputStream = new FileOutputStream(file)) {
			for (byte[] data : dataList) {
				outputStream.write(data);
			}
		}
	}

	private List<byte[]> readFile(File file) throws FileNotFoundException, IOException {
		List<byte[]> dataList = new ArrayList<>();
		try (InputStream inputStream = new FileInputStream(file)) {
			int read = 0;
			do {
				byte[] buffer = new byte[CRYPTO_CHUNK_LENGTH];
				read = inputStream.read(buffer);
				if (read == CRYPTO_CHUNK_LENGTH) {
					dataList.add(buffer);
				}
			} while (read == CRYPTO_CHUNK_LENGTH);
		}
		return dataList;
	}

	private String getDecrypted() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, Exception {
		File pathToSelfJar = getPathToSelfJar();
		KeyPair keyPair = loadKeysFromResources(ConfigHandler.class);
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		File saveFolder = new File(pathToSelfJar.getParentFile().getAbsolutePath() + File.separator + saveFolderName);
		File cryptedConfig = new File(saveFolder.getAbsolutePath() + File.separator + cryptedConfigName);
		List<byte[]> crypted = readFile(cryptedConfig);
		String decrypted = decrypt(privateKey, crypted);
		return decrypted;
	}

	public static List<List<String>> getArgList(String[] args) {
		List<String> argList = Arrays.asList(args);
		List<List<String>> argList2 = new ArrayList<>();
		argList2.add(new ArrayList<>());
		for (String arg : argList) {
			if (arg.equals(SPLITTER)) {
				argList2.add(new ArrayList<>());
			} else {
				argList2.get(argList2.size() - 1).add(arg);
			}
		}
		return argList2;
	}

	public static String getParameter(String param, List<String> argList) {
		return argList.contains(param) ? argList.get(argList.indexOf(param) + 1) : null;
	}

	public static Map<String, String> getParameters(String param, List<String> argList) {
		int i = 0;
		Map<String, String> map = new HashMap<>();
		while (i < argList.size()) {
			if (argList.get(i).startsWith(param)) {
				map.put(argList.get(i), argList.get(i + 1));
				i++;
			}
			i++;
		}
		return map;
	}
}
