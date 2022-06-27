package com.utils.ssh.bean;

import java.util.List;

/**
 * @author NAnishhenko
 */
public class ExecCommandBean {

	private final List<String> responseData;
	private final List<String> responseErrorData;

	public ExecCommandBean(List<String> responseData, List<String> responseErrorData) {
		this.responseData = responseData;
		this.responseErrorData = responseErrorData;
	}

	public List<String> getResponseData() {
		return responseData;
	}

	public List<String> getResponseErrorData() {
		return responseErrorData;
	}

}
