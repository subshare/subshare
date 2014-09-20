package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.Date;

import javax.jdo.Query;

import org.subshare.core.sign.Signable;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.Entity;

public class SignableDao extends Dao<Entity, SignableDao> { // since Sinable is an interface implemented by many Entity-classes, we cannot type this correctly ;-)

	public boolean isEntitiesSignedByAndAfter(final Uid signingUserRepoKeyId, final Date signatureCreatedAfter) {
		assertNotNull("signingUserRepoKeyId", signingUserRepoKeyId);
		assertNotNull("signatureCreatedAfter", signatureCreatedAfter);
		// TODO this does not contain file chunks! We need to add them, too!

		@SuppressWarnings("rawtypes")
		final Collection<Class> entityClasses = pm().getPersistenceManagerFactory().getManagedClasses();
		for (final Class<?> entityClass : entityClasses) {
			if (Signable.class.isAssignableFrom(entityClass)) {
				@SuppressWarnings("unchecked")
				final
				Class<? extends Signable> signableEntityClass = (Class<? extends Signable>) entityClass;
				final long count = getEntitiesCountSignedByAndAfter(signableEntityClass, signingUserRepoKeyId, signatureCreatedAfter);
				if (count > 0)
					return true;
			}
		}
		return false;
	}

	private long getEntitiesCountSignedByAndAfter(final Class<? extends Signable> signableEntityClass, final Uid signingUserRepoKeyId, final Date signatureCreatedAfter) {
		final Query q = pm().newQuery(signableEntityClass);
		q.setResult("count(this)");
		q.setFilter("this.signingUserRepoKeyId == :signingUserRepoKeyId && this.signatureCreated > :signatureCreatedAfter");
		final Long result = (Long) q.execute(signingUserRepoKeyId.toString(), signatureCreatedAfter);
		return result;
	}

}
