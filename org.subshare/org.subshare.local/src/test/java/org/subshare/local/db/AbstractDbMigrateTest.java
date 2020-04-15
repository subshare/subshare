package org.subshare.local.db;

import org.junit.AfterClass;

import org.subshare.local.AbstractTest;

public abstract class AbstractDbMigrateTest extends AbstractTest {

	@AfterClass
	public static void after_AbstractDbMigrateTest() { // make sure that it is always reset!
		disablePostgresql();
	}

}
