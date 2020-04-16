package org.subshare.local.db;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.core.dto.SignatureDto;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.local.db.DatabaseMigrater;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class SsDatabaseMigrater extends DatabaseMigrater {
	private static final Logger logger = LoggerFactory.getLogger(SsDatabaseMigrater.class);

	protected SsDatabaseMigrater(File localRoot) {
		super(localRoot);
	}

	protected void testTargetPersistence() throws Exception {
		logger.info("testTargetPersistence: entered.");
		super.testTargetPersistence();
		requireNonNull(targetPm, "targetPm");
		
		targetPm.currentTransaction().begin();
		try {
			CryptoRepoFileDao crfDao = getDao(CryptoRepoFileDao.class);
			CryptoKeyDao ckDao = getDao(CryptoKeyDao.class);
			RepoFileDao rfDao = getDao(RepoFileDao.class);

			RepoFile rootDir = rfDao.getChildRepoFile(null, "");
			requireNonNull(rootDir, "rootDir");
			
			if (! (rootDir instanceof Directory))
				throw new IllegalStateException("rootDir is an instance of " + rootDir.getClass().getName() + ", but it must be an instance of Directory: " + rootDir);
			
			CryptoRepoFile rootCryptoRepoFile = crfDao.getCryptoRepoFile(rootDir);
			requireNonNull(rootCryptoRepoFile, "rootCryptoRepoFile");
			
			CryptoKey ck = createObject(CryptoKey.class);
			ck.setCryptoKeyRole(CryptoKeyRole.dataKey);
			ck.setCryptoKeyType(CryptoKeyType.symmetric);
			ck.setCryptoRepoFile(rootCryptoRepoFile);
			
			SignatureDto signature = new SignatureDto();
			signature.setSignatureCreated(new Date());
			signature.setSignatureData(new byte[3]);
			signature.setSigningUserRepoKeyId(new Uid());
			ck.setSignature(signature);

			ckDao.makePersistent(ck);
			ckDao.getPersistenceManager().flush();
		} finally {
			targetPm.currentTransaction().rollback();
		}

	}
}
