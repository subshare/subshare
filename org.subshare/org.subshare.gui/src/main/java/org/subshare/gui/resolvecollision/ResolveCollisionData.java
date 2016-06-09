package org.subshare.gui.resolvecollision;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.resolvecollision.collision.CollisionData;

import co.codewizards.cloudstore.core.dto.Uid;

public class ResolveCollisionData {

	private LocalRepo localRepo;
	private Set<Uid> collisionIds;
	private List<CollisionData> collisionDatas;

	public ResolveCollisionData(LocalRepo localRepo, Set<Uid> collisionIds) {
		this.localRepo = assertNotNull("localRepo", localRepo);
		this.collisionIds = assertNotNull("collisionIds", collisionIds);
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}

	public Set<Uid> getCollisionIds() {
		return collisionIds;
	}

	public List<CollisionData> getCollisionDatas() {
		if (collisionDatas == null)
			collisionDatas = new ArrayList<>();

		return collisionDatas;
	}
}
