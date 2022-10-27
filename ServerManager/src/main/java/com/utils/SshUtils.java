package com.utils;

import com.utils.ssh.SshClient;
import com.utils.ssh.bean.ExecCommandBean;
import com.utils.ssh.bean.OsEnum;
import java.nio.file.Path;
import java.util.ArrayList;

public class SshUtils {

	public static final OsEnum currensOs = OsEnum.LINUX;
	private final String host;
	private final String user;
	private final String password;
	private final int port;

	public SshUtils(String host, String user, String password, int port) {
		this.host = host;
		this.user = user;
		this.password = password;
		this.port = port;
	}

	public void uploadFiles(Path from, Path to) throws Exception {
		Path root = to.getRoot();
		ArrayList<Path> pathListToCheck = new ArrayList<>();
		pathListToCheck.add(to);
		Path path = to;
		while (!path.equals(root)) {
			path = path.getParent();
			pathListToCheck.add(path);
		}
		SshClient sshClient = new SshClient(host, user, password, port, !currensOs.equals(OsEnum.AIX));
		ArrayList<Path> pathListToCreate = new ArrayList<>();
		for (int i = pathListToCheck.size() - 1; i >= 0; i--) {
			ExecCommandBean execCommand = sshClient.execCommand(new String[]{"cd " + pathListToCheck.get(i).toAbsolutePath().toString()});
			if (execCommand.getResponseErrorData().stream().filter(str -> str != null && str.length() > 0).count() > 0L) {
				pathListToCreate.add(pathListToCheck.get(i));
			}
		}
		for (Path pathToCreate : pathListToCreate) {
			ExecCommandBean execCommand = sshClient.execCommand(new String[]{"mkdir " + pathToCreate.toAbsolutePath().toString()});
		}
		sshClient.scpTo(to.toString(), from.toString());
	}

	public ExecCommandBean execute(String command) throws Exception {
		SshClient sshClient = new SshClient(host, user, password, port, !currensOs.equals(OsEnum.AIX));
		ExecCommandBean execCommand = sshClient.execCommand(new String[]{command});
		return execCommand;
	}

	public ExecCommandBean executeAsync(String command) throws Exception {
		SshClient sshClient = new SshClient(host, user, password, port, !currensOs.equals(OsEnum.AIX));
		ExecCommandBean execCommand = sshClient.execCommandAsync(new String[]{command});
		return execCommand;
	}

	public ExecCommandBean executeScript(Path script) throws Exception {
		SshClient sshClient = new SshClient(host, user, password, port, !currensOs.equals(OsEnum.AIX));
		ExecCommandBean execCommand = sshClient.execCommandAsync(new String[]{"sh " + script.toAbsolutePath().toString() + " &"});
		return execCommand;
	}
}
