package org.subshare.server;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;
import static org.subshare.core.repair.RepairConst.*;

import java.util.UUID;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.ls.server.SsLocalServer;
import org.subshare.rest.server.SubShareRest;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistryImpl;
import co.codewizards.cloudstore.local.RepairDatabase;
import co.codewizards.cloudstore.ls.server.LocalServer;
import co.codewizards.cloudstore.server.CloudStoreServer;

public class SubShareServer extends CloudStoreServer {

	private static final Logger logger = LoggerFactory.getLogger(SubShareServer.class);

	public static void main(final String[] args) throws Exception {
		setCloudStoreServerClass(SubShareServer.class);
		CloudStoreServer.main(args);
	}

	public SubShareServer(final String... args) {
		super(args);
	}

	@Override
	protected LocalServer createLocalServer() {
		return new SsLocalServer();
	}

	@Override
	protected ResourceConfig createResourceConfig() {
		return new SubShareRest();
	}

	@Override
	protected Server createServer() {
		repairIfNeeded();
		return super.createServer();
	}

	protected void repairIfNeeded() {
		try {
			LocalRepoRegistry localRepoRegistry = LocalRepoRegistryImpl.getInstance();
			for (UUID repositoryId : localRepoRegistry.getRepositoryIds()) {
				logger.info("repairIfNeeded: repositoryId={}", repositoryId);
				File localRoot = localRepoRegistry.getLocalRoot(repositoryId);
				if (localRoot == null) {
					logger.warn("repairIfNeeded: repositoryId={}: localRoot not found!", repositoryId);
					continue;
				}

				File repairTriggerFile = createFile(localRoot, META_DIR_NAME).createFile(REPAIR_TRIGGER_FILE_NAME);
				boolean repairEnabled = ! repairTriggerFile.exists();
				if (repairEnabled) {
					logger.info("repairIfNeeded: repositoryId={}, repairTriggerFile='{}' does not exist! Creating this file and beginning repair now.", repositoryId, repairTriggerFile.getAbsolutePath());
					try {
						repairTriggerFile.createOutputStream().close();
						new RepairDatabase(localRoot).run();

						LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
						localRepoManager.close();

						logger.info("repairIfNeeded: repositoryId={}: Repair completed.", repositoryId);
					} catch (Exception x) {
						logger.error("repairIfNeeded: repositoryId=" + repositoryId + ": " + x, x);
					}
				}
				else
					logger.info("repairIfNeeded: repositoryId={}, repairTriggerFile='{}' exists! Skipping repair.", repositoryId, repairTriggerFile.getAbsolutePath());
			}
		} catch (Exception x) {
			logger.error("repairIfNeeded: " + x, x);
		}
	}
}
