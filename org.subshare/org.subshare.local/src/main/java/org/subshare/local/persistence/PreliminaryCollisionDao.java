package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.Dao;

public class PreliminaryCollisionDao extends Dao<PreliminaryCollision, PreliminaryCollisionDao> {

	public PreliminaryCollision getPreliminaryCollision(final String path) {
		assertNotNull("path", path);
		final String pathSha1 = sha1(path);
		final Query query = pm().newNamedQuery(getEntityClass(), "getPreliminaryCollision_pathSha1");
		try {
			final PreliminaryCollision result = (PreliminaryCollision) query.execute(pathSha1);
			return result;
		} finally {
			query.closeAll();
		}
	}

}
