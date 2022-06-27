package com.utils.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.utils.ssh.bean.ExecCommandBean;
import com.utils.ssh.bean.MyUserInfo;
import java.io.ByteArrayOutputStream;

/**
 *
 * @author NAnishhenko
 */
public class SshClient {

	private final String host;
	private final String user;
	private final String password;
	private final int port;
	private final boolean ptimestamp;

	public SshClient(String host, String user, String password, int port, boolean ptimestamp) {
		this.host = host;
		this.user = user;
		this.password = password;
		this.port = port;
		this.ptimestamp = ptimestamp;
	}

	public ExecCommandBean execCommand(String[] commands) throws Exception {
		JSch jsch = new JSch();
		ExecCommandBean execCommand = null;
		List<String> responseData = new ArrayList<>();
		Session session = jsch.getSession(user, host, port);
		try {
			session.setDaemonThread(true);
			session.setPassword(password);
			UserInfo ui = new MyUserInfo(password);
			session.setUserInfo(ui);
			session.connect();
			for (String command : commands) {
				execCommand = execCommand(session.openChannel("exec"), command);
			}
		} finally {
			session.disconnect();
		}
		return execCommand != null ? execCommand : new ExecCommandBean(new ArrayList<>(1), new ArrayList<>(1));
	}

	private ExecCommandBean execCommand(Channel channel, String command) throws JSchException, IOException {
		((com.jcraft.jsch.ChannelExec) channel).setCommand(command);
		channel.setInputStream(null);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		((com.jcraft.jsch.ChannelExec) channel).setErrStream(byteArrayOutputStream);
		BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
		channel.connect();
		byte[] tmp = new byte[1024];
		List<String> responseData = new ArrayList<>();
		while (true) {
			String line;
			while ((line = in.readLine()) != null) {
				responseData.add(line.replaceAll("\\s+", " ").trim());
			}

			if (channel.isClosed()) {
				//                System.out.println("exit-status: " + channel.getExitStatus());
				break;
			}
		}
		channel.disconnect();
		return new ExecCommandBean(responseData, Arrays.asList(new String(byteArrayOutputStream.toByteArray()).split("\\n")));
	}

	public ExecCommandBean execCommandAsync(String[] commands) throws Exception {
		JSch jsch = new JSch();
		ExecCommandBean execCommand = null;
		List<String> responseData = new ArrayList<>();
		Session session = jsch.getSession(user, host, port);
		try {
			session.setDaemonThread(true);
			session.setPassword(password);
			UserInfo ui = new MyUserInfo(password);
			session.setUserInfo(ui);
			session.connect();
			for (String command : commands) {
				execCommand = execCommandAsync(session.openChannel("exec"), command);
			}
		} finally {
			session.disconnect();
		}
		return execCommand != null ? execCommand : new ExecCommandBean(new ArrayList<>(1), new ArrayList<>(1));
	}

	private ExecCommandBean execCommandAsync(Channel channel, String command) throws JSchException, IOException {
		((com.jcraft.jsch.ChannelExec) channel).setCommand(command);
		channel.setInputStream(null);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		((com.jcraft.jsch.ChannelExec) channel).setErrStream(null);
//		BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
		channel.connect();
		byte[] tmp = new byte[1024];
		List<String> responseData = new ArrayList<>();
//		while (true) {
//			String line;
//			while ((line = in.readLine()) != null) {
//				responseData.add(line.replaceAll("\\s+", " ").trim());
//			}
//
//			if (channel.isClosed()) {
//				//                System.out.println("exit-status: " + channel.getExitStatus());
//				break;
//			}
//		}
		channel.disconnect();
		return new ExecCommandBean(responseData, Arrays.asList(new String(byteArrayOutputStream.toByteArray()).split("\\n")));
	}

	// В такой реализации это приводит к блокировкам и утечкам потоков!
//	public ExecCommandBean execCommand(String[] commands) throws Exception {
//		JSch jsch = new JSch();
//		ExecCommandBean execCommand = null;
//		Session session = jsch.getSession(user, host, port);
//		try {
//			session.setDaemonThread(true);
//			session.setPassword(password);
//			UserInfo ui = new MyUserInfo(password);
//			session.setUserInfo(ui);
//			session.connect();
//			for (String command : commands) {
//				execCommand = execCommand(session.openChannel("exec"), command);
//			}
//		} finally {
//			session.disconnect();
//		}
//		return execCommand != null ? execCommand : new ExecCommandBean(new ArrayList<>(1), new ArrayList<>(1));
//	}
//
//	private ExecCommandBean execCommand(Channel channel, String command) throws JSchException, IOException {
//		((com.jcraft.jsch.ChannelExec) channel).setCommand(command);
////		channel.setInputStream(null);
//		((com.jcraft.jsch.ChannelExec) channel).setErrStream(System.out);
//		InputStream in = channel.getInputStream();
//		InputStream in2 = ((com.jcraft.jsch.ChannelExec) channel).getErrStream();
//		channel.connect();
//		List<String> responseData = new ArrayList<>();
//		List<String> responseErrorData = new ArrayList<>();
//		while (true) {
//			String line;
////            while ((line = in.readLine()) != null) {
////                responseData.add(line.replaceAll("\\s+", " ").trim());
////            }
////            while ((line = in2.readLine()) != null) {
////                responseErrorData.add(line.replaceAll("\\s+", " ").trim());
////            }
//			responseData.addAll(copyAvailable(in));
//			responseErrorData.addAll(copyAvailable(in2));
//			if (channel.isClosed()) {
//				//                System.out.println("exit-status: " + channel.getExitStatus());
//				break;
//			}
//		}
//		channel.disconnect();
//		return new ExecCommandBean(responseData, responseErrorData);
//	}
	private static final int MAX_DATA_LENGTH = 20 * 1024 * 1024;

	private List<String> copyAvailable(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		byte[] buffer = new byte[1024 * 1024];
		while (is.available() > 0) {
			int l = is.available();
			l = is.read(buffer, 0, Integer.min(l, buffer.length));
			if (sb.length() <= MAX_DATA_LENGTH) {
				sb.append(new String(buffer, 0, l));
			}
		}
		String str = sb.toString();
		return str == null || str.length() == 0 ? new ArrayList<>(1) : Arrays.asList(str.split("\n"));
	}

	public void scpFrom(String rfile, String lfile) {
		//      System.err.println("usage: java ScpFrom user@remotehost:file1 file2");
		FileOutputStream fos = null;
		try {
			String prefix = null;
			if (new File(lfile).isDirectory()) {
				prefix = lfile + File.separator;
			}
			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, port);
			session.setDaemonThread(true);
			// username and password will be given via UserInfo interface.
			UserInfo ui = new MyUserInfo(password);
			session.setUserInfo(ui);
			session.connect();
			// exec 'scp -f rfile' remotely
			String command = "scp -f " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			channel.connect();
			byte[] buf = new byte[1024];
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}
				// read '0644 '
				in.read(buf, 0, 5);
				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ') {
						break;
					}
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}
				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}
				//System.out.println("filesize="+filesize+", file="+file);
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
				// read a content of lfile
				fos = new FileOutputStream(prefix == null ? lfile : prefix + file);
				int foo;
				while (true) {
					if (buf.length < filesize) {
						foo = buf.length;
					} else {
						foo = (int) filesize;
					}
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error 
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L) {
						break;
					}
				}
				fos.close();
				fos = null;
				if (checkAck(in) != 0) {
					System.exit(0);
				}
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}
			session.disconnect();
			//            System.exit(0);
		} catch (Exception e) {
			System.out.println(e);
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (Exception ee) {
			}
		}
	}

	public void scpTo(String rfile, String lfile) {
		//            System.err.println("usage: java ScpTo file1 user@remotehost:file2");
		FileInputStream fis = null;
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, port);
			session.setDaemonThread(true);
			// username and password will be given via UserInfo interface.
			UserInfo ui = new MyUserInfo(password);
			session.setUserInfo(ui);
			session.connect();
			//            boolean ptimestamp = false;
			// exec 'scp -t rfile' remotely
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			channel.connect();
			if (checkAck(in) != 0) {
				//                System.exit(0);
				return;
			}
			File _lfile = new File(lfile);
			if (ptimestamp) {
				command = "T" + (_lfile.lastModified() / 1000) + " 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					//                System.exit(0);
					return;
				}
			}
			// send "C0644 filesize filename", where filename should not include '/'
			long filesize = _lfile.length();
			command = "C0644 " + filesize + " ";
			if (lfile.lastIndexOf('/') > 0) {
				command += lfile.substring(lfile.lastIndexOf('/') + 1);
			} else {
				command += lfile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				//                System.exit(0);
				return;
			}
			// send a content of lfile
			fis = new FileInputStream(lfile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0) {
					break;
				}
				out.write(buf, 0, len); //out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				//                System.exit(0);
				return;
			}
			out.close();
			channel.disconnect();
			session.disconnect();
			//            System.exit(0);
		} catch (Exception e) {
			System.out.println(e);
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (Exception ee) {
			}
		}
	}

	private int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if (b == 0) {
			return b;
		}
		if (b == -1) {
			return b;
		}

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			//            if (b == 1) { // error
			//                System.out.print(sb.toString());
			//            }
			//            if (b == 2) { // fatal error
			//                System.out.print(sb.toString());
			//            }
		}
		return b;
	}

	public void forwardL(int rport, String rhost, int lport) throws JSchException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(user, host, port);
		session.setDaemonThread(true);
		session.setPassword(password);
		UserInfo ui = new MyUserInfo(password);
		session.setUserInfo(ui);
		session.connect();
		int assinged_port = session.setPortForwardingL(lport, rhost, rport);
		System.out.println("localhost:" + assinged_port + " -> " + rhost + ":" + rport);
	}

	public void forwardR(int rport, String lhost, int lport) throws JSchException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(user, host, port);
		session.setDaemonThread(true);
		session.setPassword(password);
		UserInfo ui = new MyUserInfo(password);
		session.setUserInfo(ui);
		session.connect();
		session.setPortForwardingR(rport, lhost, lport);
		System.out.println(host + ":" + rport + " -> " + lhost + ":" + lport);
	}

}
