package org.subshare.test;

import static co.codewizards.cloudstore.local.db.DatabaseAdapterFactory.*;
import static co.codewizards.cloudstore.local.db.ExternalJdbcDatabaseAdapter.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.local.db.DatabaseAdapterFactoryRegistry;

public class PostgresqlRepoToRepoSyncIT extends RepoToRepoSyncIT {

	@BeforeClass
	public static void before_PostgresqlBasicRepoToRepoSyncIT() {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME, "postgresql");

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME, getEnvOrFail("TEST_PG_HOST_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME, getEnvOrFail("TEST_PG_USER_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD, getEnvOrFail("TEST_PG_PASSWORD"));

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX, "TEST_SS_");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX, "_TEST");
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	@AfterClass
	public static void after_PostgresqlBasicRepoToRepoSyncIT() {
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME);

		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD);

		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX);
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	protected static String getEnvOrFail(String key) {
		String value = System.getenv(key);
		if (value == null)
			throw new IllegalStateException("Environment-variable not set: " + key);

		return value;
	}

	@Test
	public void syncFromLocalToRemoteToLocal() throws Exception {
		super.syncFromLocalToRemoteToLocal();
	}

	@Test
	public void syncFromLocalToRemoteToLocalThenDeleteFileAndSyncAgain() throws Exception {
		super.syncFromLocalToRemoteToLocalThenDeleteFileAndSyncAgain();
	}

	@Test
	public void syncFromLocalToRemoteToLocalAfterCreateAndDeleteFile() throws Exception {
		super.syncFromLocalToRemoteToLocalAfterCreateAndDeleteFile();
	}

//	@Ignore("Still working on this - collisions are still not supported!") // TODO they are now! Is this test scenario already covered in a Collision*RepoToRepoSyncIT class?
//	@Test
//	public void syncFromLocalToRemoteToLocalThenCauseDeleteCollisionOnServerDuringUpSync() throws Exception {
//		super.syncFromLocalToRemoteToLocalThenCauseDeleteCollisionOnServerDuringUpSync();
//	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefix() throws Exception {
		super.syncFromLocalToRemoteToLocalWithPathPrefix();
	}

	@Test
	public void multiSyncFromLocalToRemoteToLocalWithPathPrefixWithSubdirClearanceKey() throws Exception {
		super.multiSyncFromLocalToRemoteToLocalWithPathPrefixWithSubdirClearanceKey();
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithSubdirClearanceKey() throws Exception {
		super.syncFromLocalToRemoteToLocalWithPathPrefixWithSubdirClearanceKey();
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithWritePermissionGrantedAndRevoked() throws Exception {
		super.syncFromLocalToRemoteToLocalWithPathPrefixWithWritePermissionGrantedAndRevoked();
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithoutSubdirClearanceKey() throws Exception {
		super.syncFromLocalToRemoteToLocalWithPathPrefixWithoutSubdirClearanceKey();
	}

}
