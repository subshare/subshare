package org.subshare.core.repo.histo;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.core.repo.sync.PaddingUtil.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.repo.transport.CryptreeRestRepoTransport;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.LocalRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

public class HistoExporterImpl implements HistoExporter {

	protected final File localRoot;
	protected final URL remoteRoot;
	protected final LocalRepoManager localRepoManager;
	protected final LocalRepoTransport localRepoTransport;
	protected final CryptreeRestRepoTransport remoteRepoTransport;
	protected final UUID localRepositoryId;
	protected final UUID remoteRepositoryId;

	public static HistoExporter createHistoExporter(final File localRoot) {
		assertNotNull("localRoot", localRoot);
		return new HistoExporterImpl(localRoot);
	}

	protected HistoExporterImpl(final File localRoot) {
		this.localRoot = assertNotNull("localRoot", localRoot);
		this.localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		this.localRepositoryId = localRepoManager.getRepositoryId();
		if (localRepositoryId == null)
			throw new IllegalStateException("localRepoManager.getRepositoryId() returned null!");

		final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
		this.remoteRoot = localRepoMetaData.getRemoteRoot();
		this.remoteRepositoryId = localRepoManager.getRemoteRepositoryIdOrFail(remoteRoot);

		this.remoteRepoTransport = (CryptreeRestRepoTransport) createRepoTransport(remoteRoot, localRepositoryId);
		this.localRepoTransport = (LocalRepoTransport) createRepoTransport(localRoot, remoteRepositoryId);
	}

	private RepoTransport createRepoTransport(final File rootFile, final UUID clientRepositoryId) {
		URL rootURL;
		try {
			rootURL = rootFile.toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return createRepoTransport(rootURL, clientRepositoryId);
	}

	private RepoTransport createRepoTransport(final URL remoteRoot, final UUID clientRepositoryId) {
		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot);
		return repoTransportFactory.createRepoTransport(remoteRoot, clientRepositoryId);
	}

	@Override
	public void close() {
		localRepoManager.close();
		localRepoTransport.close();
		remoteRepoTransport.close();
	}

	protected Cryptree getCryptree(LocalRepoTransaction tx) {
		final String remotePathPrefix = ""; //$NON-NLS-1$ // TODO is this really fine?! If so, we should explain, why! And we should test!!!

		final UserRepoKeyRing userRepoKeyRing = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup().getUserRepoKeyRing(
				new UserRepoKeyRingLookupContext(localRepositoryId, remoteRepositoryId));

		final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
				tx, remoteRepositoryId, remotePathPrefix, userRepoKeyRing);

		return cryptree;
	}

	@Override
	public void exportFile(final Uid histoCryptoRepoFileId, final File exportDirectory) throws IOException {
		// TODO use param-object
		// TODO support directories and take options into account (e.g. entire dirs [complete snapshots])
		assertNotNull("histoCryptoRepoFileId", histoCryptoRepoFileId);
		assertNotNull("exportDirectory", exportDirectory);

		try (final LocalRepoTransaction tx = localRepoManager.beginReadTransaction();) {
			final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto = getCryptree(tx).getPlainHistoCryptoRepoFileDto(histoCryptoRepoFileId);
			final RepoFileDto repoFileDto = plainHistoCryptoRepoFileDto.getRepoFileDto();
			assertNotNull("plainHistoCryptoRepoFileDto.repoFileDto", repoFileDto);

			if (!(repoFileDto instanceof NormalFileDto))
				throw new UnsupportedOperationException("NYI");

			final NormalFileDto normalFileDto = (NormalFileDto) repoFileDto;

			File exportFile = createFile(exportDirectory, normalFileDto.getName());
			try (final RandomAccessFile raf = exportFile.createRandomAccessFile("rw")) {
				for (final FileChunkDto fileChunkDto : assertNotNull("normalFileDto.fileChunkDtos", normalFileDto.getFileChunkDtos())) {
					// TODO first try to get the chunk from the current local file - only download it, if it's different or missing locally!
					// TODO check, which chunks already contain the data - and skip downloading the histoFileData, whenever possible.
					byte[] histoFileData = remoteRepoTransport.getHistoFileData(histoCryptoRepoFileId, fileChunkDto.getOffset());
					histoFileData = removePadding(histoFileData);
					raf.seek(fileChunkDto.getOffset());
					raf.write(histoFileData);
				}
				raf.setLength(normalFileDto.getLength()); // in case the file existed and was too long, we need to truncate.
			}
		}
	}
}
