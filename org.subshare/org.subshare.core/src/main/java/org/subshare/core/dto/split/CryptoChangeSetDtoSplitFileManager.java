package org.subshare.core.dto.split;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.jaxb.CryptoChangeSetDtoIo;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.IOUtil;

public class CryptoChangeSetDtoSplitFileManager {

	protected static final String CRYPTO_CHANGE_SET_DTO_DIR_SUFFIX = ".CryptoChangeSetDto";
//	protected static final String CRYPTO_CHANGE_SET_DTO_TMP_DIR_SUFFIX = ".tmp";
	protected static final String CRYPTO_CHANGE_SET_DTO_TMP_DIR_INFIX = ".tmp.";
	protected static final String CRYPTO_CHANGE_SET_DTO_FILE_PREFIX = "CryptoChangeSetDto.";
	protected static final String CRYPTO_CHANGE_SET_DTO_FILE_SUFFIX = ".xml.gz";
	protected static final String CRYPTO_CHANGE_SET_DTO_FILE_IMPORTED_SUFFIX = ".imported";
	protected static final String REPO_TEMP_DIR_NAME = "tmp";

	protected static final String PROP_KEY_LAST_CRYPTO_KEY_SYNC_TO_REMOTE_REPO_LOCAL_REPOSITORY_REVISION_SYNCED = "lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced";

	private final LocalRepoManager localRepoManager;
	private final UUID remoteRepositoryId;

	private File baseDir;
	private File cryptoChangeSetDtoTmpDir;
//	private boolean cryptoChangeSetDtoTmpDirRandom;
	private File cryptoChangeSetDtoFinalDir;

	protected CryptoChangeSetDtoSplitFileManager(final LocalRepoManager localRepoManager, final UUID remoteRepositoryId) {
		this.localRepoManager = assertNotNull(localRepoManager, "localRepoManager");
		this.remoteRepositoryId = assertNotNull(remoteRepositoryId, "remoteRepositoryId");
	}

	/**
	 * Create an instance of {@code CryptoChangeSetDtoSplitFileManager}.
	 * @param localRepoManager the local-repo-manager. Must not be <code>null</code>.
	 * @param remoteRepositoryId the repository-ID of the opposite side (i.e. the client-side, if we're on the server;
	 * or the server-side, if we're on the client).
	 * @return an instance of {@code CryptoChangeSetDtoSplitFileManager}.
	 */
	public static CryptoChangeSetDtoSplitFileManager createInstance(final LocalRepoManager localRepoManager, final UUID remoteRepositoryId) {
		return new CryptoChangeSetDtoSplitFileManager(localRepoManager, remoteRepositoryId);
	}

	/**
	 * Gets the base-directory within the local-repo's meta-directory.
	 * @return the base-directory within the local-repo's meta-directory. Never <code>null</code>.
	 * @throws IOException if creating this directory failed or any other IO-problem occurred.
	 */
	protected File getBaseDir() throws IOException {
		if (baseDir == null) {
			final File metaDir = getMetaDir();
			if (! metaDir.isDirectory()) {
				if (metaDir.isFile())
					throw new IOException(String.format("Path '%s' already exists as ordinary file! It should be a directory!", metaDir.getAbsolutePath()));
				else
					throw new IOException(String.format("Directory '%s' does not exist!", metaDir.getAbsolutePath()));
			}

			final File baseDir = metaDir.createFile(REPO_TEMP_DIR_NAME);
			if (! baseDir.isDirectory()) {
				baseDir.mkdir();

				if (! baseDir.isDirectory()) {
					if (baseDir.isFile())
						throw new IOException(String.format("Cannot create directory '%s', because this path already exists as an ordinary file!", baseDir.getAbsolutePath()));
					else
						throw new IOException(String.format("Creating directory '%s' failed for an unknown reason (permissions? disk full?)!", baseDir.getAbsolutePath()));
				}
			}
			this.baseDir = baseDir;
		}
		return baseDir;
	}

	protected File getCryptoChangeSetDtoFinalDir() throws IOException {
		if (cryptoChangeSetDtoFinalDir == null)
			cryptoChangeSetDtoFinalDir = getBaseDir().createFile(remoteRepositoryId.toString() + CRYPTO_CHANGE_SET_DTO_DIR_SUFFIX);

		return cryptoChangeSetDtoFinalDir;
	}

	protected File getCryptoChangeSetDtoTmpDir() throws IOException {
		if (cryptoChangeSetDtoTmpDir == null) {
			final File finalDir = getCryptoChangeSetDtoFinalDir();
//			if (isCryptoChangeSetDtoTmpDirRandom())
				cryptoChangeSetDtoTmpDir = IOUtil.createUniqueRandomFolder(finalDir.getParentFile(), finalDir.getName() + CRYPTO_CHANGE_SET_DTO_TMP_DIR_INFIX);
//			else {
//				cryptoChangeSetDtoTmpDir = finalDir.getParentFile().createFile(finalDir.getName() + CRYPTO_CHANGE_SET_DTO_TMP_DIR_SUFFIX);
//
//				if (! cryptoChangeSetDtoTmpDir.isDirectory()) {
//					cryptoChangeSetDtoTmpDir.mkdir();
//
//					if (! cryptoChangeSetDtoTmpDir.isDirectory())
//						throw new IOException("Directory does not exist and could not be created: " + cryptoChangeSetDtoTmpDir.getAbsolutePath());
//				}
//			}
		}
		return cryptoChangeSetDtoTmpDir;
	}

	public void writeCryptoChangeSetDtos(final List<CryptoChangeSetDto> cryptoChangeSetDtos, final Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced) throws IOException {
		assertNotNull(cryptoChangeSetDtos, "cryptoChangeSetDtos");

		if (cryptoChangeSetDtos.isEmpty())
			throw new IllegalArgumentException("cryptoChangeSetDtos empty");

		final File finalDir = getCryptoChangeSetDtoFinalDir();
		if (finalDir.exists())
			throw new IllegalStateException(String.format("Directory '%s' exists, but should not!", finalDir.getAbsolutePath()));

		final File tmpDir = getCryptoChangeSetDtoTmpDir();

		final CryptoChangeSetDtoIo dtoIo = new CryptoChangeSetDtoIo();
		int expectedMultiPartIndex = -1;
		for (final CryptoChangeSetDto cryptoChangeSetDto : cryptoChangeSetDtos) {
			final int multiPartIndex = cryptoChangeSetDto.getMultiPartIndex();
			if (++expectedMultiPartIndex != multiPartIndex)
				throw new IllegalArgumentException(String.format("Wrong multiPartIndex! expectedMultiPartIndex=%s, multiPartIndex=%s", expectedMultiPartIndex, multiPartIndex));

			final File file = tmpDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex));
			if (file.exists())
				throw new IllegalStateException("File already exists: " + file.getAbsolutePath());

			dtoIo.serializeWithGz(cryptoChangeSetDto, file);
		}

		final Properties properties = new Properties();
		properties.setProperty(PROP_KEY_LAST_CRYPTO_KEY_SYNC_TO_REMOTE_REPO_LOCAL_REPOSITORY_REVISION_SYNCED,
				lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced == null ? "" : lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced.toString());
		try (final IOutputStream out = getCryptoChangeSetDtoPropertiesFile(tmpDir).createOutputStream()) {
			properties.store(castStream(out), null);
		}

		if (! tmpDir.renameTo(finalDir))
			throw new IOException(String.format("Renaming '%s' to '%s' failed!", tmpDir.getAbsolutePath(), finalDir.getAbsolutePath()));
	}

	public Long readLastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced() throws IOException {
		final File finalDir = getCryptoChangeSetDtoFinalDir();
		if (! finalDir.exists())
			return null;

		final File propertiesFile = getCryptoChangeSetDtoPropertiesFile(finalDir);
		if (! propertiesFile.exists())
			return null;

		final Properties properties = new Properties();
		try (final IInputStream in = propertiesFile.createInputStream()) {
			properties.load(castStream(in));
		}
		final String s = properties.getProperty(PROP_KEY_LAST_CRYPTO_KEY_SYNC_TO_REMOTE_REPO_LOCAL_REPOSITORY_REVISION_SYNCED);
		if (s == null || s.isEmpty())
			return null;

		final long result = Long.parseLong(s.trim());
		return result;
	}

	protected File getCryptoChangeSetDtoPropertiesFile(final File dir) {
		return dir.createFile(CRYPTO_CHANGE_SET_DTO_FILE_PREFIX + ".properties");
	}

	protected String getCryptoChangeSetDtoFileName(int multiPartIndex) {
		return CRYPTO_CHANGE_SET_DTO_FILE_PREFIX + multiPartIndex + CRYPTO_CHANGE_SET_DTO_FILE_SUFFIX;
	}

	public CryptoChangeSetDto readCryptoChangeSetDto(int multiPartIndex) throws IOException {
		final int cryptoChangeSetDtoFinalFileCount = getFinalFileCount();
		if (cryptoChangeSetDtoFinalFileCount < 1)
			throw new IllegalStateException("No multi-part-CryptoChangeSetDto-files prepared!");

		if (cryptoChangeSetDtoFinalFileCount <= multiPartIndex)
			throw new IllegalArgumentException("multiPartIndex out of range!");

		final File finalDir = getCryptoChangeSetDtoFinalDir();
		final File file = finalDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex));

		final CryptoChangeSetDtoIo dtoIo = new CryptoChangeSetDtoIo();
		final CryptoChangeSetDto result = dtoIo.deserializeWithGz(file);
		return result;
	}

	public byte[] readCryptoChangeSetDtoFile(int multiPartIndex) throws IOException {
		final int cryptoChangeSetDtoFinalFileCount = getFinalFileCount();
		if (cryptoChangeSetDtoFinalFileCount < 1)
			throw new IllegalStateException("No multi-part-CryptoChangeSetDto-files prepared!");

		if (cryptoChangeSetDtoFinalFileCount <= multiPartIndex)
			throw new IllegalArgumentException("multiPartIndex out of range!");

		final File finalDir = getCryptoChangeSetDtoFinalDir();
		final File file = finalDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex));

		final long fileLength = file.length();
		if (fileLength > Integer.MAX_VALUE)
			throw new IllegalStateException(String.format("File '%s' too large!", file.getAbsolutePath()));

		final byte[] result = new byte[(int) fileLength];
		try (IInputStream in = file.createInputStream()) {
			readOrFail(castStream(in), result, 0, result.length);
		}
		return result;
	}

	public void deleteCryptoChangeSetDtoFinalDir() throws IOException {
		final File finalDir = getCryptoChangeSetDtoFinalDir();

		if (finalDir.exists())
			finalDir.deleteRecursively();

		if (finalDir.exists())
			throw new IOException(String.format("Deleting '%s' recursively failed!", finalDir.getAbsolutePath()));
	}

	private File getMetaDir() {
		return createFile(localRepoManager.getLocalRoot(), LocalRepoManager.META_DIR_NAME);
	}

	public int getFinalFileCount() throws IOException {
		final File finalDir = getCryptoChangeSetDtoFinalDir();
		if (! finalDir.exists())
			return 0;

		final String[] fileNames = finalDir.list();
		if (fileNames == null)
			return 0;

		int result = 0;
		for (final String fileName : fileNames) {
			if (fileName.startsWith(CRYPTO_CHANGE_SET_DTO_FILE_PREFIX)
					&& fileName.endsWith(CRYPTO_CHANGE_SET_DTO_FILE_SUFFIX))
				++result;
		}
		return result;
	}

//	public boolean isCryptoChangeSetDtoTmpDirRandom() {
//		return cryptoChangeSetDtoTmpDirRandom;
//	}
//	public void setCryptoChangeSetDtoTmpDirRandom(boolean cryptoChangeSetDtoTmpDirRandom) {
//		if (cryptoChangeSetDtoTmpDir != null)
//			throw new IllegalStateException("Too late! Cannot change flag after cryptoChangeSetDtoTmpDir already initialized.");
//
//		this.cryptoChangeSetDtoTmpDirRandom = cryptoChangeSetDtoTmpDirRandom;
//	}

	public boolean existsCryptoChangeSetDtoFile(int multiPartIndex) throws IOException {
		final File finalDir = getCryptoChangeSetDtoFinalDir();
		final File finalFile = finalDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex));
		return finalFile.exists();
	}

	public void writeCryptoChangeSetDtoFile(int multiPartIndex, byte[] fileData) throws IOException {
		final File tmpDir = getCryptoChangeSetDtoTmpDir();
		final File tmpFile = tmpDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex));

		final File finalDir = getCryptoChangeSetDtoFinalDir();
		final File finalFile = finalDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex));

		if (! finalDir.isDirectory()) {
			finalDir.mkdir();

			if (! finalDir.isDirectory())
				throw new IOException("Creating directory failed: " + finalDir.getAbsolutePath());
		}

		try (IOutputStream out = tmpFile.createOutputStream()) {
			out.write(fileData);
		}
		finalFile.deleteRecursively();
		if (! tmpFile.renameTo(finalFile))
			throw new IOException(String.format("Renaming '%s' to '%s' failed!", tmpFile.getAbsolutePath(), finalFile.getAbsolutePath()));
	}

	public void markCryptoChangeSetDtoImported(int multiPartIndex) throws IOException {
		final File finalDir = getCryptoChangeSetDtoFinalDir();
		final File finalFile = finalDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex));
		if (! finalFile.isFile())
			throw new IllegalArgumentException("Cannot mark nonexistent file: " + finalFile.getAbsolutePath());

		final File markerFile = finalDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex) + CRYPTO_CHANGE_SET_DTO_FILE_IMPORTED_SUFFIX);
		markerFile.createNewFile();

		if (! markerFile.isFile())
			throw new IOException("Creating file failed: " + markerFile.getAbsolutePath());
	}

	public boolean isCryptoChangeSetDtoImported(int multiPartIndex) throws IOException {
		final File finalDir = getCryptoChangeSetDtoFinalDir();
		final File finalFile = finalDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex));
		if (! finalFile.isFile())
			throw new IllegalArgumentException("Nonexistent file: " + finalFile.getAbsolutePath());

		final File markerFile = finalDir.createFile(getCryptoChangeSetDtoFileName(multiPartIndex) + CRYPTO_CHANGE_SET_DTO_FILE_IMPORTED_SUFFIX);
		return markerFile.isFile();
	}

	public void deleteAll() throws IOException {
		final String startsWithFileNameMask = remoteRepositoryId.toString() + CRYPTO_CHANGE_SET_DTO_DIR_SUFFIX;
		for (final File file : getBaseDir().listFiles()) {
			if (file.getName().startsWith(startsWithFileNameMask)) {
				file.deleteRecursively();
			}
		}
	}

}
