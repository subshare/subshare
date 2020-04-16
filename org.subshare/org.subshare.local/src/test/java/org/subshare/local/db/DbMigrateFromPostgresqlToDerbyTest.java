package org.subshare.local.db;

import static co.codewizards.cloudstore.local.db.DatabaseAdapterFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.local.db.DatabaseMigrater;
import co.codewizards.cloudstore.local.db.DatabaseAdapterFactoryRegistry;

public class DbMigrateFromPostgresqlToDerbyTest extends AbstractDbMigrateTest {
	private static final Logger logger = LoggerFactory.getLogger(DbMigrateFromPostgresqlToDerbyTest.class);

//	private File localRoot;
	@BeforeClass
	public static void before_DbMigrateFromPostgresqlToDerbyTest() throws Exception {
		enablePostgresql();
	}

	@Test
	public void migrateFromPostgresqlToDerby() throws Exception {
//		localRoot = newTestRepositoryLocalRoot("local");
//		assertThat(localRoot.exists()).isFalse();
//		localRoot.mkdirs();
//		assertThat(localRoot.isDirectory()).isTrue();
//
//		LocalRepoManager localRepoManagerLocal = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localRoot);
//		assertThat(localRepoManagerLocal).isNotNull();
//
//		final File child_1 = createDirectory(localRoot, "1");
//
//		createFileWithRandomContent(child_1, "a");
//		createFileWithRandomContent(child_1, "b");
//		createFileWithRandomContent(child_1, "c");
//
//		final File child_2 = createDirectory(localRoot, "2");
//
//		createFileWithRandomContent(child_2, "a");
//
//		final File child_2_1 = createDirectory(child_2, "1");
//		createFileWithRandomContent(child_2_1, "a");
//		createFileWithRandomContent(child_2_1, "b", 150000);
//
//		final File child_3 = createDirectory(localRoot, "3");
//
//		createFileWithRandomContent(child_3, "a");
//		createFileWithRandomContent(child_3, "b");
//		createFileWithRandomContent(child_3, "c");
//		createFileWithRandomContent(child_3, "d");
//
//		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));

		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			UUID repositoryId = localRepoManager.getRepositoryId();
			logger.info("local repo: {}", repositoryId);
	
			localRepoManager.setCloseDeferredMillis(0); // close it immediately!
		}

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME, "derby");
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();

		DatabaseMigrater databaseMigrater = DatabaseMigrater.create(localRoot);
		databaseMigrater.deleteTriggerFile();
		databaseMigrater.migrateIfNeeded();
	}

}
