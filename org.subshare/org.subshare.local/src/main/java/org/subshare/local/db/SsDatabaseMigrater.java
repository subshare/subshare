package org.subshare.local.db;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.core.dto.SignatureDto;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.PreliminaryCollision;
import org.subshare.local.persistence.PreliminaryCollisionDao;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.local.db.DatabaseMigrater;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
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
		
		targetPm.close();
		targetPm = targetPmf.getPersistenceManager();

		long preliminaryCollisionId = -1;
		final String testPath = "test/test/bla/blubb/sdsdoc0e9fvevjiorvhurthß0rejfosdojoivjiiosfvhuidvhdifuh/fvei9hruvuibfvuibfvuidbfvzubfzubvfdv/sfvuidfvhiudhvuifdhuih/dslkvjiofvjuifhvuihuihf/sdlvnfduihvuidhffuivhfduivuirtgvtrgvzurevggfdzusgzusgdvzusd/sdvjnweuirvgrtgvzubfrvzurgvzugdcfdf/wiojferiojiorejfjuifdbvufbv/sdjknfibvuirbtvzutrgvgfvezubgdsugcsduiguidgdcsd/sdhcih9we89zreure8w78refsdiuhapicnsdiohowuhcwuhuisdahouisdhchsdcuih/sdjbvsdbvu";
		targetPm.currentTransaction().begin();
		try {
			PreliminaryCollisionDao pcDao = getDao(PreliminaryCollisionDao.class);
			
			PreliminaryCollision preliminaryCollision = new PreliminaryCollision();
			preliminaryCollision.setChanged(new Date());
			preliminaryCollision.setPath(testPath);
			
			pcDao.makePersistent(preliminaryCollision);
			preliminaryCollisionId = preliminaryCollision.getId();
			if (preliminaryCollisionId < 0)
				throw new IllegalStateException("preliminaryCollisionId < 0");
			
			targetPm.currentTransaction().commit();
			targetPm.evictAll();
		} finally {
			if (targetPm.currentTransaction().isActive())
				targetPm.currentTransaction().rollback();
		}
		
		targetPm.close();
		targetPm = targetPmf.getPersistenceManager();

		targetPm.currentTransaction().begin();
		try {
			PreliminaryCollisionDao pcDao = getDao(PreliminaryCollisionDao.class);

			PreliminaryCollision preliminaryCollision = pcDao.getObjectByIdOrFail(preliminaryCollisionId);
			requireNonNull(preliminaryCollision.getPath(), "preliminaryCollision.path");
			if (! testPath.equals(preliminaryCollision.getPath()))
				throw new IllegalStateException("testPath != preliminaryCollision.path :: '" + testPath + "' != '" + preliminaryCollision.getPath() + "'");

			pcDao.deletePersistent(preliminaryCollision);

			targetPm.currentTransaction().commit();
			targetPm.evictAll();
		} finally {
			if (targetPm.currentTransaction().isActive())
				targetPm.currentTransaction().rollback();
		}
		
		targetPm.close();
		targetPm = targetPmf.getPersistenceManager();

		targetPm.currentTransaction().begin();
		try {
			PreliminaryCollisionDao pcDao = getDao(PreliminaryCollisionDao.class);

			PreliminaryCollision preliminaryCollision = pcDao.getObjectByIdOrNull(preliminaryCollisionId);
			if (preliminaryCollision != null)
				throw new IllegalStateException("PreliminaryCollision was not deleted!");

		} finally {
			targetPm.currentTransaction().rollback();
		}
	}
}
