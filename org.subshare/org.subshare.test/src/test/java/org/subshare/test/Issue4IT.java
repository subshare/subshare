package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.rest.client.transport.CryptreeRestRepoTransportImpl;

import co.codewizards.cloudstore.core.oio.File;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class Issue4IT extends AbstractRepoToRepoSyncIT {

	private File fileToBeDeleted;

	@Override
	public void before() throws Exception {
		super.before();

		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			void createUserIdentities(UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};

		new MockUp<CryptreeRestRepoTransportImpl>() {
			@Mock
			void putFileData(Invocation invocation, final String path, final long offset, final byte[] fileData) {
				System.out.println("******************** path: " + path);

				if ("/2/a".equals(path) && fileToBeDeleted.isFile()) {
					fileToBeDeleted.delete();
					assertThat(fileToBeDeleted.getIoFile()).doesNotExist();
					throw new TestException();
				}

				invocation.proceed();
			}
		};
	}

	@Test
	public void issue_4_syncFileDeletedAfterLocalSync() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();

		fileToBeDeleted = createFile(localSrcRoot, "2").createFile("a");
		assertThat(fileToBeDeleted.getIoFile()).isFile();

		try {
			syncFromLocalSrcToRemote();
			fail("TestException not thrown!");
		} catch (TestException x) {
			doNothing(); // expected!
		}

		syncFromLocalSrcToRemote();
	}

	@SuppressWarnings("serial")
	public static class TestException extends RuntimeException {
	}
}
