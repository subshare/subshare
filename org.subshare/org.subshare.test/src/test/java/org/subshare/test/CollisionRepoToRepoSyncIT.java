package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CollisionDto;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

import co.codewizards.cloudstore.core.oio.File;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public abstract class CollisionRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {
	private static final Logger logger = LoggerFactory.getLogger(CollisionRepoToRepoSyncIT.class);

	@Override
	public void before() throws Exception {
		super.before();

		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			void createUserIdentities(UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};
	}

	protected static void modifyFile_append(File file, int byteToAppend) throws IOException {
		try (OutputStream out = file.createOutputStream(true)) { // append
			out.write(byteToAppend);
		}
	}

	protected static void modifyFile_append(File file, int byteToAppend, int length) throws IOException {
		try (OutputStream out = new BufferedOutputStream(file.createOutputStream(true))) { // append
			for (int i = 0; i < length; ++i)
				out.write(byteToAppend);
		}
	}

	protected static CollisionDto getCollisionDtoWithDuplicateCryptoRepoFileIdOrFail(Collection<CollisionDto> collisionDtos) {
		CollisionDto result = null;
		for (CollisionDto collisionDto : collisionDtos) {
			if (collisionDto.getDuplicateCryptoRepoFileId() != null) {
				if (result == null)
					result = collisionDto;
				else
					throw new IllegalArgumentException("collisionDtos contains multiple elements with duplicateCryptoRepoFileId != null: " + collisionDtos);
			}
		}

		if (result == null)
			throw new IllegalArgumentException("collisionDtos contains no element with duplicateCryptoRepoFileId != null: " + collisionDtos);

		return result;
	}

	protected void prepareLocalAndDestinationRepo() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		syncFromRemoteToLocalDest();
	}

	protected int getLastByte(File file) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file.getIoFile(), "r")) {
			raf.seek(raf.length() - 1);
			int result = raf.read();
			assertThat(result).isGreaterThanOrEqualTo(0);
			return result;
		}
	}
}
