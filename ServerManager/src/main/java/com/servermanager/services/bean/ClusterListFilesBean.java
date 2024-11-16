package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import com.servermanager.services.FilesClusterService;
import java.io.File;
import java.util.List;
import java.util.Map;

public class ClusterListFilesBean<T> extends TransferObject<T> {

	private Map<String, List<File>> listFiles;
	private transient final StartServerManager startServerManager;

	public ClusterListFilesBean(StartServerManager startServerManager) {
		this.startServerManager = startServerManager;
	}

	public ClusterListFilesBean(Map<String, List<File>> listFiles, StartServerManager startServerManager) {
		this.listFiles = listFiles;
		this.startServerManager = startServerManager;
	}

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		FilesClusterService clusterService = startServerManager.getClusterService();
		if (clusterService != null) {
			Map<String, List<File>> listFiles = clusterService.listFiles();
			return new ClusterListFilesBean(listFiles, startServerManager);
		} else {
			return new TransferObject();
		}
	}

	public Map<String, List<File>> getListFiles() {
		return listFiles;
	}
}
