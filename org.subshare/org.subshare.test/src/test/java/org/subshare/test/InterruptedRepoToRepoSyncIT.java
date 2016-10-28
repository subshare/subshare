package org.subshare.test;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.sync.SsRepoToRepoSync;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.repo.transport.LocalRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import mockit.Mock;
import mockit.MockUp;

public class InterruptedRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {

	private static final Logger logger = LoggerFactory.getLogger(InterruptedRepoToRepoSyncIT.class);

	private int putFileDataInSyncUpInvocationCountBeforeException;
	private int putFileDataInSyncDownInvocationCountBeforeException;

	@Override
	public void before() throws Exception {
		super.before();

		putFileDataInSyncUpInvocationCountBeforeException = -1;
		putFileDataInSyncDownInvocationCountBeforeException = -1;

		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			private void createUserIdentities(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};
	}

	@Test
	public void interruptSyncUpAndResume() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();

		putFileDataInSyncUpInvocationCountBeforeException = 3;

		try {
			syncFromLocalSrcToRemote();
			fail("InterruptedSyncException was not thrown");
		} catch (InterruptedSyncException x) {
			doNothing(); // expected!
		}

		// resume again.
		putFileDataInSyncUpInvocationCountBeforeException = -1;
		syncFromLocalSrcToRemote();

		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		syncFromRemoteToLocalDest();
	}

	@Test
	public void interruptSyncDownAndResume() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();

		putFileDataInSyncDownInvocationCountBeforeException = 3;

		try {
			syncFromRemoteToLocalDest();
			fail("InterruptedSyncException was not thrown");
		} catch (InterruptedSyncException x) {
			doNothing(); // expected!
		}

		// resume again.
		putFileDataInSyncDownInvocationCountBeforeException = -1;
		syncFromRemoteToLocalDest();
	}

	@Override
	protected RepoToRepoSync createRepoToRepoSync(File localRoot, URL remoteRoot) {
		return new InterruptableRepoToRepoSync(localRoot, remoteRoot);
	}

	@Override
	protected void syncFromRemoteToLocalDest() throws Exception {
		super.syncFromRemoteToLocalDest();
	}

	protected class InterruptableRepoToRepoSync extends SsRepoToRepoSync {
		private int putFileDataInSyncUpInvocationCount;
		private int putFileDataInSyncDownInvocationCount;

		public InterruptableRepoToRepoSync(File localRoot, URL remoteRoot) {
			super(localRoot, remoteRoot);
		}

		@Override
		protected void putFileData(RepoTransport fromRepoTransport,
				RepoTransport toRepoTransport,
				RepoFileDtoTreeNode repoFileDtoTreeNode, String path,
				FileChunkDto fileChunkDto, byte[] fileData) {

			if (fromRepoTransport instanceof LocalRepoTransport) { // up
				if (toRepoTransport instanceof LocalRepoTransport)
					throw new IllegalStateException("WTF?!");

				if (putFileDataInSyncUpInvocationCountBeforeException >= 0
						&& ++putFileDataInSyncUpInvocationCount > putFileDataInSyncUpInvocationCountBeforeException)
					throw new InterruptedSyncException();
			}
			else if (toRepoTransport instanceof LocalRepoTransport) { // down
				if (putFileDataInSyncDownInvocationCountBeforeException >= 0
						&& ++putFileDataInSyncDownInvocationCount > putFileDataInSyncDownInvocationCountBeforeException)
					throw new InterruptedSyncException();
			}
			else
				throw new IllegalStateException("WTF?!");

			super.putFileData(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, fileChunkDto, fileData);
		}
	}

	protected static final class InterruptedSyncException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
