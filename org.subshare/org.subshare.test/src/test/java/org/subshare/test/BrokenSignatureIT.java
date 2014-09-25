package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static mockit.Deencapsulation.*;
import static org.assertj.core.api.Assertions.*;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;
import org.subshare.core.user.UserRepoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.rest.client.transport.CryptreeRepoTransport;
import org.subshare.rest.server.service.SsWebDavService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.local.LocalRepoTransactionImpl;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.DirectoryDao;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.NormalFileDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.rest.client.RemoteException;

public class BrokenSignatureIT extends AbstractRepoToRepoSyncIT {

	private static final Logger logger = LoggerFactory.getLogger(BrokenSignatureIT.class);

	private boolean jmockitShouldBeUsed;
	private boolean jmockitWasUsed;
	private boolean cryptreeRepoTransport_encryptAndSign_breakSignature = false;
	private boolean ssWebDavService_getFileData_breakSignature = false;

	@Override
	public void before() {
		super.before();
		jmockitShouldBeUsed = false;
		jmockitWasUsed = false;
		cryptreeRepoTransport_encryptAndSign_breakSignature = false;
	}

	@Override
	public void after() {
		super.after();
		if (jmockitShouldBeUsed && ! jmockitWasUsed)
			fail("jmockit should have been used but was not used! missing agent?");
	}

	private final int headerAreaLengthCausingDifferentException = 5 + 4;
	private final int footerAreaLengthCausingDifferentException = 2;

	@Test
	public void uploadBrokenSignature() throws Exception {
		jmockitShouldBeUsed = true;

		new MockUp<CryptreeRepoTransport>() {
			@Mock
			public byte[] encryptAndSign(final Invocation invocation, final byte[] plainText, final KeyParameter keyParameter, final UserRepoKey signingUserRepoKey) {
				jmockitWasUsed = true;

				logger.info("encryptAndSign: about to call invocation.proceed(...). cryptreeRepoTransport_encryptAndSign_breakSignature={}", cryptreeRepoTransport_encryptAndSign_breakSignature);
				final byte[] result = invocation.proceed(plainText, keyParameter, signingUserRepoKey);
				if (cryptreeRepoTransport_encryptAndSign_breakSignature) {
					// If we modify anything in this part of the header, we cause a different exception - not a SignatureException!
					// The same goes for the last few bytes of the footer.
					final int index =
							headerAreaLengthCausingDifferentException +
							random.nextInt(result.length - headerAreaLengthCausingDifferentException - footerAreaLengthCausingDifferentException);
					result[index] += 1;
					logger.info("encryptAndSign: modified result[{}] => signature should be broken!", index);
				}
				return result;
			}
		};

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();

		// Because a test runs *very* long, we do not test every possible broken signature scenario in every test run.
		// Most signature verifications happen generically, anyway (=> VerifySignableAndWriteProtectedEntityListener).
		// Instead, we pick one random scenario and break only exactly one signature.

		final int caseRandom = random.nextInt(450);

		if (caseRandom < 50) // generic, same as all Crypto* entities => lower probability
			breakRandomEntitySignature(localSrcRoot, new CryptoKeyDao());
		else if (caseRandom < 100) // generic, same as all Crypto* entities => lower probability
			breakRandomEntitySignature(localSrcRoot, new CryptoLinkDao());
		else if (caseRandom < 150) // generic, same as all Crypto* entities => lower probability
			breakRandomEntitySignature(localSrcRoot, new CryptoRepoFileDao());
		else if (caseRandom < 250) // generic, but signature generated on the fly directly before web-service-invocation
			breakRandomNormalFileDtoSignatureForUpload();
		else if (caseRandom < 350) // generic, but signature generated on the fly directly before web-service-invocation
			breakRandomDirectoryDtoSignatureForUpload();
		else // totally different; signature is around a stream (=> VerifierInputStream)
			breakRandomFileDataForUpload();

		logger.info("uploadBrokenSignature: about to invoke syncFromLocalSrcToRemote() with broken signature.");

		try {
			syncFromLocalSrcToRemote();
			fail("The broken signature was not detected by the server!");
		} catch (final SignatureException x) {
			logger.info("Caught expected SignatureException: " + x, x);
			final RemoteException remoteException = ExceptionUtil.getCause(x, RemoteException.class);
			if (remoteException == null)
				fail("The broken signature was detected by the client; it should have been sent to the server and detected there! We have to change our code or this test!");
		}
	}

	@Test
	public void downloadBrokenSignature() throws Exception {
		jmockitShouldBeUsed = true;

		new MockUp<SsWebDavService>() {
			@Mock
			public byte[] getFileData(
					final Invocation invocation,
					final String path,
					final long offset,
					final int length) {
				jmockitWasUsed = true;

				logger.info("getFileData: about to call invocation.proceed(...). ssWebDavService_getFileData_breakSignature={}", ssWebDavService_getFileData_breakSignature);
				final byte[] result = invocation.proceed(path, offset, length);
				if (ssWebDavService_getFileData_breakSignature) {
					// If we modify anything in this part of the header, we cause a different exception - not a SignatureException!
					// The same goes for the last few bytes of the footer.
					final int index =
							headerAreaLengthCausingDifferentException +
							random.nextInt(result.length - headerAreaLengthCausingDifferentException - footerAreaLengthCausingDifferentException);
					result[index] += 1;
					logger.info("getFileData: modified result[{}] => signature should be broken!", index);
				}
				return result;
			}
		};

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();

		// Because a test runs *very* long, we do not test every possible broken signature scenario in every test run.
		// Most signature verifications happen generically, anyway (=> VerifySignableAndWriteProtectedEntityListener).
		// Instead, we pick one random scenario and break only exactly one signature.

		final int caseRandom = random.nextInt(450);

		if (caseRandom < 50)
			breakRandomEntitySignature(remoteRoot, new CryptoKeyDao());
		else if (caseRandom < 100)
			breakRandomEntitySignature(remoteRoot, new CryptoLinkDao());
		else if (caseRandom < 150)
			breakRandomEntitySignature(remoteRoot, new CryptoRepoFileDao());
		else if (caseRandom < 250)
			breakRandomEntitySignature(remoteRoot, new NormalFileDao());
		else if (caseRandom < 350)
			breakRandomEntitySignature(remoteRoot, new DirectoryDao());
		else
			breakRandomFileDataForDownload();

		try {
			syncFromLocalSrcToRemote();
			fail("The broken signature was not detected by the client!");
		} catch (final SignatureException x) {
			logger.info("Caught expected SignatureException: " + x, x);
			final RemoteException remoteException = ExceptionUtil.getCause(x, RemoteException.class);
			if (remoteException != null)
				fail("The broken signature was detected by the server; it should have been sent to the client and detected there! We have to change our code or this test!");
		}
	}

	private <T> void copySignatureFromOneRandomElementToAnotherRandomElement(final List<T> list) {
		final List<T> twoRandomElements = getTwoRandomElements(list);

		final T element1 = twoRandomElements.get(0);
		final T element2 = twoRandomElements.get(1);

		assertThat(element2).isNotSameAs(element1);
		assertThat(element2).isNotEqualTo(element1);

		final Signable signable1 = (Signable) element1;
		final Signable signable2 = (Signable) element2;

		signable1.setSignature(SignatureDto.copyIfNeeded(signable2.getSignature()));

		((AutoTrackLocalRevision)signable1).setLocalRevision(Long.MAX_VALUE);
		if (signable1 instanceof RepoFile)
			((RepoFile) signable1).setLastSyncFromRepositoryId(null);
	}

	private <T> List<T> getTwoRandomElements(final List<T> list) {
		if (list.size() < 2)
			throw new IllegalArgumentException("list.size() < 2");

		final int index1 = random.nextInt(list.size());
		int index2 = index1;
		while (index2 == index1)
			index2 = random.nextInt(list.size());

		final List<T> result = new ArrayList<T>(2);
		result.add(list.get(index1));
		result.add(list.get(index2));
		return result;
	}

	private void breakRandomFileDataForDownload() throws Exception {
		ssWebDavService_getFileData_breakSignature = true;

		Uid cryptoRepoFileId;

		// We change a chunk on the client-side, because the server has the chunks encrypted only.
		// It's thus easier to fake on the client side and only make sure the system believes, the change
		// originates from the server.
		PersistenceManager pm = getTransactionalPersistenceManager(localSrcRoot);
		try {
			final CryptoRepoFileDao cryptoRepoFileDao = new CryptoRepoFileDao().persistenceManager(pm);
			final NormalFileDao normalFileDao = new NormalFileDao().persistenceManager(pm);

			final List<NormalFile> normalFiles = (List<NormalFile>) normalFileDao.getObjects();
			final NormalFile normalFile = normalFiles.get(random.nextInt(normalFiles.size()));

			normalFile.setSha1(modifySha1(normalFile.getSha1()));

			final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(normalFile);
			cryptoRepoFileId = cryptoRepoFile.getCryptoRepoFileId();

			final FileChunk fileChunk = normalFile.getFileChunks().iterator().next();
			final String sha1 = fileChunk.getSha1();

			final String fieldName = "sha1";
			setField(fileChunk, fieldName, modifySha1(sha1));
			JDOHelper.makeDirty(fileChunk, fieldName);

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();
		}

		pm = getTransactionalPersistenceManager(remoteRoot);
		try {
			final CryptoRepoFileDao cryptoRepoFileDao = new CryptoRepoFileDao().persistenceManager(pm);
			final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(cryptoRepoFileId);
			final RepoFile repoFile = assertNotNull("cryptoRepoFile.repoFile", cryptoRepoFile.getRepoFile());

			repoFile.setLocalRevision(Long.MAX_VALUE);
			repoFile.setLastSyncFromRepositoryId(null);

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();
		}
	}

	private String modifySha1(final String sha1) {
		char sha1FirstChar = sha1.charAt(0);
		final String sha1Suffix = sha1.substring(1);

		if (++sha1FirstChar > 'f')
			sha1FirstChar = '0';

		return sha1FirstChar + sha1Suffix;
	}

	private void breakRandomFileDataForUpload() throws Exception {
		cryptreeRepoTransport_encryptAndSign_breakSignature = true;

		final File file = pickRandomFile(localSrcRoot, false);
		logger.info("breakRandomFileDataForUpload: file='{}'", file.getAbsolutePath());
		final OutputStream out = file.createOutputStream(true);
		out.write(200);
		out.close();
		file.setLastModified(System.currentTimeMillis() + 1500); // making absolutely sure, the timestamp is different
	}

	private File pickRandomFile(final File localRoot, final boolean directory) {
		File file = null;
		final List<File> files = listRecursively(localRoot);
		while (file == null) {
			file = files.get(random.nextInt(files.size()));
			if (file.isDirectory() && !directory)
				file = null;
		}
		return file;
	}

	private List<File> listRecursively(final File dir) {
		final List<File> files = new ArrayList<File>();
		populateListRecursively(files, dir);
		return files;
	}

	private void populateListRecursively(final List<File> files, final File file) {
		if (LocalRepoManager.META_DIR_NAME.equals(file.getName()))
			return;

		files.add(file);
		final File[] children = file.listFiles();
		if (children != null) {
			for (final File child : children)
				populateListRecursively(files, child);
		}
	}

	private void breakRandomNormalFileDtoSignatureForUpload() throws Exception {
		new MockUp<SsNormalFileDto>() {
			private SignatureDto signatureDto;

			@Mock
			public Signature getSignature() {
				logger.info("getSignature: entered");
				jmockitWasUsed = true;
				return this.signatureDto;
			}

			@Mock
			public void setSignature(final Signature signature) {
				jmockitWasUsed = true;
				final SignatureDto signatureDto = SignatureDto.copyIfNeeded(signature);
				if (signatureDto != null) {
					final byte[] signatureData = signatureDto.getSignatureData();
					final int index = random.nextInt(signatureData.length);
					signatureData[index] += 1;
					logger.info("setSignature: modified signatureData[{}] => signature should be broken!", index);
				}
				this.signatureDto = signatureDto;
			}
		};

		final File child_2 = createFile(localSrcRoot, "2");
		final File child_2_a = createFile(child_2, "a");
		final OutputStream out = child_2_a.createOutputStream(true);
		out.write(200);
		out.close();
	}

	private void breakRandomDirectoryDtoSignatureForUpload() {
		new MockUp<SsDirectoryDto>() {
			private SignatureDto signatureDto;

			@Mock
			public Signature getSignature() {
				logger.info("getSignature: entered");
				jmockitWasUsed = true;
				return this.signatureDto;
			}

			@Mock
			public void setSignature(final Signature signature) {
				jmockitWasUsed = true;
				final SignatureDto signatureDto = SignatureDto.copyIfNeeded(signature);
				if (signatureDto != null) {
					final byte[] signatureData = signatureDto.getSignatureData();
					final int index = random.nextInt(signatureData.length);
					signatureData[index] += 1;
					logger.info("setSignature: modified signatureData[{}] => signature should be broken!", index);
				}
				this.signatureDto = signatureDto;
			}
		};

		final File child_2 = createFile(localSrcRoot, "2");
		child_2.setLastModified(child_2.lastModified() - 1000);
	}

	private <E extends Entity> void breakRandomEntitySignature(final File localRoot, final Dao<E, ?> dao) {
		final PersistenceManager pm = getTransactionalPersistenceManager(localRoot);
		try {
			dao.persistenceManager(pm);
			final List<E> entities = (List<E>) dao.getObjects(); // we know that it is a list ;-)
			copySignatureFromOneRandomElementToAnotherRandomElement(entities);

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();
		}
	}

	private PersistenceManager getTransactionalPersistenceManager(final File localRoot) {
		final PersistenceManagerFactory pmf;
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				pmf = ((LocalRepoTransactionImpl)transaction).getPersistenceManager().getPersistenceManagerFactory();
			}
		}
		final PersistenceManager pm = pmf.getPersistenceManager();
		pm.currentTransaction().begin();
		return pm;
	}
}
