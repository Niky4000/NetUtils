package com.servermanager.services;

import static com.utils.FileUtils.getPathToJar;
import com.utils.SshUtils;
import com.utils.ssh.bean.ExecCommandBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class UpdateSshService extends AbstractService {

	private final String user;
	private final String password;

	public UpdateSshService(String host, String user, String password, int port) {
		super(host, port);
		this.user = user;
		this.password = password;
	}

	public void update(Path to, String command) throws Exception {
		new Thread(() -> {
			try {
				Path from = Paths.get(getPathToJar().getAbsolutePath());
				SshUtils sshUtils = new SshUtils(host, user, password, port);
				ExecCommandBean execute = sshUtils.execute("ps -ef | grep " + from.toFile().getName());
				execute.getResponseData().stream().filter(str -> str.contains("java") && str.contains("-jar") && str.contains(from.toFile().getName())).forEach(process -> {
					Long processId = Optional.ofNullable(process).map(str -> str.split(" ")).filter(arr -> arr.length > 1).map(arr -> arr[1]).map(Long::valueOf).orElse(null);
					if (processId != null) {
						try {
							ExecCommandBean execute2 = sshUtils.execute("kill -9 " + processId.toString());
							execute2.getResponseData().forEach(System.out::println);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
				sshUtils.uploadFiles(from, to);
				sshUtils.executeAsync(command);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).start();
	}
}
