package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import com.servermanager.services.FilesClusterService;
import java.io.File;
import java.util.List;
import java.util.Map;

public class ClusterListFilesBean<T> extends TransferObject<T> {

	Map<String, List<File>> listFiles;

	public ClusterListFilesBean() {
	}

	public ClusterListFilesBean(Map<String, List<File>> listFiles) {
		this.listFiles = listFiles;
	}

	@Override
	public TransferObject apply(TransferObject<T> object) {
		FilesClusterService clusterService = StartServerManager.getClusterService();
		if (clusterService != null) {
			Map<String, List<File>> listFiles = clusterService.listFiles();
			return new ClusterListFilesBean(listFiles);
		} else {
			return new TransferObject();
		}
	}

	public Map<String, List<File>> getListFiles() {
		return listFiles;
	}
}
