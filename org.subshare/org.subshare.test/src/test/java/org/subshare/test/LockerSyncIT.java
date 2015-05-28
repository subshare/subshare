package org.subshare.test;

import java.net.URL;

import org.subshare.core.locker.LockerSync;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerImpl;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Just started - there's no real test code, yet!")
public class LockerSyncIT extends AbstractIT {

	@Test
	public void testLockerSync() throws Exception {
		Server server = new ServerImpl();
		server.setUrl(new URL(getSecureUrl()));

		// TODO implement this!
		LockerSync lockerSync = new LockerSync(server);
		lockerSync.sync();

	}

}
