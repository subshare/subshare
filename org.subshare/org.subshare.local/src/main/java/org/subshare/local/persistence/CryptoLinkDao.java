package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoLinkDto;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.FetchPlanBackup;

public class CryptoLinkDao extends Dao<CryptoLink, CryptoLinkDao> {
	private static final Logger logger = LoggerFactory.getLogger(CryptoLinkDao.class);

	public CryptoLink getCryptoLinkOrFail(final Uid cryptoLinkId) {
		final CryptoLink cryptoLink = getCryptoLink(cryptoLinkId);
		if (cryptoLink == null)
			throw new IllegalArgumentException("There is no CryptoLink with this cryptoLinkId: " + cryptoLinkId);

		return cryptoLink;
	}

	public CryptoLink getCryptoLink(final Uid cryptoLinkId) {
		assertNotNull(cryptoLinkId, "cryptoLinkId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoLink_cryptoLinkId");
		try {
			final CryptoLink cryptoLink = (CryptoLink) query.execute(cryptoLinkId.toString());
			return cryptoLink;
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Get those {@link CryptoLink}s whose {@link CryptoLink#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well. Note, that this is only
	 * necessary, if CryptoKey/CryptoLink instances are mutable. Currently, they seem never to be changed again.
	 * @param localRevision the {@link CryptoLink#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 *
	 * @return those {@link CryptoLink}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<CryptoLink> getCryptoLinksChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		assertNotNull(exclLastSyncFromRepositoryId, "exclLastSyncFromRepositoryId");

		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getCryptoLinksChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoLink> result = (Collection<CryptoLink>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getCryptoLinksChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			fetchPlanBackup.restore(pm);
			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getCryptoLinksChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	public List<CryptoLinkDto> getCryptoLinkDtosChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		assertNotNull(exclLastSyncFromRepositoryId, "exclLastSyncFromRepositoryId");

		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getCryptoLinksChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoLink> result = (Collection<CryptoLink>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getCryptoLinkDtosChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			List<CryptoLinkDto> resultDtos = loadDtos(result);
			logger.debug("getCryptoLinkDtosChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", resultDtos.size(), System.currentTimeMillis() - startTimestamp);

			return resultDtos;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	protected List<CryptoLinkDto> loadDtos(Collection<CryptoLink> entities) {
		return super.loadDtos(entities, CryptoLinkDto.class,
				"this.cryptoLinkId, this.fromCryptoKey.cryptoKeyId, this.fromUserRepoKeyPublicKey.userRepoKeyId, "
				+ "this.toCryptoKey.cryptoKeyId, this.toCryptoKeyPart, this.toCryptoKeyData, this.signature");
	}

	public Collection<CryptoLink> getActiveCryptoLinks(final CryptoRepoFile toCryptoRepoFile, final CryptoKeyRole toCryptoKeyRole, final CryptoKeyPart toCryptoKeyPart) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getActiveCryptoLinks_toCryptoRepoFile_toCryptoKeyRole_toCryptoKeyPart");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoLink> cryptoLinks = (Collection<CryptoLink>) query.execute(toCryptoRepoFile, toCryptoKeyRole, toCryptoKeyPart);
			logger.debug("getActiveCryptoLinks: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoLinks = load(cryptoLinks);
			logger.debug("getActiveCryptoLinks: Loading result-set with {} elements took {} ms.", cryptoLinks.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoLinks;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoLink> getActiveCryptoLinks(final CryptoRepoFile toCryptoRepoFile, final CryptoKeyRole toCryptoKeyRole, final CryptoKeyPart toCryptoKeyPart, final UserRepoKeyPublicKey fromUserRepoKeyPublicKey) {
		// TODO maybe add new DB query? even though this post-filtering should be pretty fast, too, as there are likely not many results.
		final List<CryptoLink> result = new ArrayList<CryptoLink>();
		for (final CryptoLink cryptoLink : getActiveCryptoLinks(toCryptoRepoFile, toCryptoKeyRole, toCryptoKeyPart)) {
			if (equal(fromUserRepoKeyPublicKey, cryptoLink.getFromUserRepoKeyPublicKey()))
				result.add(cryptoLink);
		}
		return result;
	}

	public Collection<CryptoLink> getCryptoLinks(final UserRepoKeyPublicKey fromUserRepoKeyPublicKey) {
		assertNotNull(fromUserRepoKeyPublicKey, "fromUserRepoKeyPublicKey");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoLinks_fromUserRepoKeyPublicKey");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoLink> cryptoLinks = (Collection<CryptoLink>) query.execute(fromUserRepoKeyPublicKey);
			logger.debug("getCryptoLinks: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoLinks = load(cryptoLinks);
			logger.debug("getCryptoLinks: Loading result-set with {} elements took {} ms.", cryptoLinks.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoLinks;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoLink> getCryptoLinksFrom(final CryptoKey fromCryptoKey) {
		assertNotNull(fromCryptoKey, "fromCryptoKey");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoLinksFrom_fromCryptoKey");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoLink> cryptoLinks = (Collection<CryptoLink>) query.execute(fromCryptoKey);
			logger.debug("getCryptoLinks: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoLinks = load(cryptoLinks);
			logger.debug("getCryptoLinks: Loading result-set with {} elements took {} ms.", cryptoLinks.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoLinks;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoLink> getCryptoLinksTo(final CryptoKey toCryptoKey) {
		assertNotNull(toCryptoKey, "toCryptoKey");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoLinksTo_toCryptoKey");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoLink> cryptoLinks = (Collection<CryptoLink>) query.execute(toCryptoKey);
			logger.debug("getCryptoLinks: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoLinks = load(cryptoLinks);
			logger.debug("getCryptoLinks: Loading result-set with {} elements took {} ms.", cryptoLinks.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoLinks;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoLink> getCryptoLinksSignedBy(final Uid signingUserRepoKeyId) {
		assertNotNull(signingUserRepoKeyId, "signingUserRepoKeyId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoLinks_signingUserRepoKeyId");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoLink> cryptoLinks = (Collection<CryptoLink>) query.execute(signingUserRepoKeyId.toString());
			logger.debug("getCryptoLinksSignedBy: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoLinks = load(cryptoLinks);
			logger.debug("getCryptoLinksSignedBy: Loading result-set with {} elements took {} ms.", cryptoLinks.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoLinks;
		} finally {
			query.closeAll();
		}
	}

//	public Collection<CryptoLink> getActiveCryptoLinks(final CryptoRepoFile toCryptoRepoFile, final CryptoKeyRole toCryptoKeyRole, final Uid fromUserRepoKeyId) {
//		final Query query = pm().newNamedQuery(getEntityClass(), "getActiveCryptoLinks_toCryptoRepoFile_toCryptoKeyRole_fromUserRepoKeyId");
//		try {
//			long startTimestamp = System.currentTimeMillis();
//			@SuppressWarnings("unchecked")
//			Collection<CryptoLink> cryptoLinks = (Collection<CryptoLink>) query.execute(toCryptoRepoFile, toCryptoKeyRole, fromUserRepoKeyId);
//			logger.debug("getActiveCryptoLinks: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);
//
//			startTimestamp = System.currentTimeMillis();
//			cryptoLinks = load(cryptoLinks);
//			logger.debug("getActiveCryptoLinks: Loading result-set with {} elements took {} ms.", cryptoLinks.size(), System.currentTimeMillis() - startTimestamp);
//
//			return cryptoLinks;
//		} finally {
//			query.closeAll();
//		}
//	}
}
