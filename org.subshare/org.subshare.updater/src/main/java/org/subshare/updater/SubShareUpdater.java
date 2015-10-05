package org.subshare.updater;

import co.codewizards.cloudstore.updater.CloudStoreUpdater;

public class SubShareUpdater extends CloudStoreUpdater {

	public static void main(String[] args) throws Exception {
		setCloudStoreUpdaterClass(SubShareUpdater.class);
		CloudStoreUpdater.main(args);
	}

	public SubShareUpdater(String[] args) {
		super(args);
	}
}
