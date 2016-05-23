package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDtoTreeNode;
import org.subshare.core.repo.local.HistoFrameFilter;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

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

	protected List<PlainHistoCryptoRepoFileDto> getPlainHistoCryptoRepoFileDtos(LocalRepoManager localRepoManager, File file) throws IOException {
		final String path = "/" + localRepoManager.getLocalRoot().relativize(file).replace('\\', '/');
		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		List<PlainHistoCryptoRepoFileDto> result = new ArrayList<>();

		// TODO need to extend the filter with a path! Do this when extending the UI to show a history in every folder-detail-pane.
		// The current implementation is very inefficient - but we have only small test data, anyway ;-)
		Collection<HistoFrameDto> histoFrameDtos = localRepoMetaData.getHistoFrameDtos(new HistoFrameFilter());
		for (HistoFrameDto histoFrameDto : histoFrameDtos) {
			PlainHistoCryptoRepoFileFilter filter = new PlainHistoCryptoRepoFileFilter();
			filter.setHistoFrameId(histoFrameDto.getHistoFrameId());
			filter.setFillParents(true);
			Collection<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = localRepoMetaData.getPlainHistoCryptoRepoFileDtos(filter);
			PlainHistoCryptoRepoFileDtoTreeNode rootNode = PlainHistoCryptoRepoFileDtoTreeNode.createTree(plainHistoCryptoRepoFileDtos);
			for (PlainHistoCryptoRepoFileDtoTreeNode node : rootNode) {
				if (path.equals(node.getPath()))
					result.add(node.getPlainHistoCryptoRepoFileDto());
			}
		}

		Collections.sort(result, new Comparator<PlainHistoCryptoRepoFileDto>() {
			@Override
			public int compare(PlainHistoCryptoRepoFileDto o1, PlainHistoCryptoRepoFileDto o2) {
				Date signatureCreated1 = o1.getHistoCryptoRepoFileDto().getSignature().getSignatureCreated();
				Date signatureCreated2 = o2.getHistoCryptoRepoFileDto().getSignature().getSignatureCreated();
				return signatureCreated1.compareTo(signatureCreated2);
			}
		});

		return result;
	}
}
