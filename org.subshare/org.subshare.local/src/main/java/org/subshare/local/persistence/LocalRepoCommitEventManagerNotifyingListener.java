package org.subshare.local.persistence;

import static java.util.Objects.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.listener.CreateLifecycleListener;
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.listener.EntityId;
import org.subshare.core.repo.listener.EntityModification;
import org.subshare.core.repo.listener.EntityModificationType;
import org.subshare.core.repo.listener.LocalRepoCommitEventManagerImpl;

import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.persistence.Entity;

public class LocalRepoCommitEventManagerNotifyingListener
	extends AbstractLocalRepoTransactionListener
	implements CreateLifecycleListener, StoreLifecycleListener, DeleteLifecycleListener
{
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoCommitEventManagerNotifyingListener.class);

	private final Map<EntityId, EntityModification> entityId2LastModification = new HashMap<>();
	private final List<EntityModification> modifications = new LinkedList<>();

	@Override
	public void onBegin() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PersistenceManager pm = ((ContextWithPersistenceManager)tx).getPersistenceManager();
		pm.addInstanceLifecycleListener(this, Object.class);
	}

	@Override
	public void onCommit() {
		if (modifications.isEmpty())
			return;

		final LocalRepoManager localRepoManager = getTransactionOrFail().getLocalRepoManager();
		final LocalRepoCommitEventManagerImpl localRepoCommitEventManagerImpl = (LocalRepoCommitEventManagerImpl) LocalRepoCommitEventManagerImpl.getInstance();
		localRepoCommitEventManagerImpl.fireLater(localRepoManager, modifications);

		modifications.clear(); // was copied ;-)
		entityId2LastModification.clear();
	}

	@Override
	public void preDelete(InstanceLifecycleEvent event) { }

	@Override
	public void postDelete(InstanceLifecycleEvent event) {
		final Object entity = requireNonNull(event.getPersistentInstance(), "event.persistentInstance");
		final EntityId entityId = getEntityId(entity);
		if (entityId != null)
			enlist(new EntityModification(entityId, EntityModificationType.DELETE));
	}

	@Override
	public void preStore(InstanceLifecycleEvent event) { }

	@Override
	public void postStore(InstanceLifecycleEvent event) {
		final Object entity = requireNonNull(event.getPersistentInstance(), "event.persistentInstance");
		final EntityId entityId = getEntityId(entity);
		if (entityId != null)
			enlist(new EntityModification(entityId, EntityModificationType.CHANGE));
	}

	@Override
	public void postCreate(InstanceLifecycleEvent event) {
		final Object entity = requireNonNull(event.getPersistentInstance(), "event.persistentInstance");
		final EntityId entityId = getEntityId(entity);
		if (entityId != null)
			enlist(new EntityModification(entityId, EntityModificationType.CREATE));
	}

	private void enlist(final EntityModification modification) {
		requireNonNull(modification, "modification");
		final EntityId entityId = modification.getEntityId();
		final EntityModification lastModification = entityId2LastModification.get(entityId);

		if (lastModification != null) {
			// We prevent duplicate *consecutive* entries - e.g. if there are a few hundred
			// changes, we register only one single change.
			if (lastModification.getType() == modification.getType())
				return;

			// If an object was newly created, we don't care about changes, because there is
			// technically no difference whether the object was first persisted and then modified
			// or first completely assembled and then finally persisted. For the outside observer
			// who is notified *after* *commit*, there's absolutely no difference, because he sees
			// a transaction atomically.
			if (lastModification.getType() == EntityModificationType.CREATE
					&& modification.getType() == EntityModificationType.CHANGE)
				return;

			if (lastModification.getType() == EntityModificationType.DELETE
					&& modification.getType() == EntityModificationType.CHANGE)
				throw new IllegalStateException("WTF?! How can a deleted entity be changed?!");

			if (lastModification.getType() == EntityModificationType.CHANGE
					&& modification.getType() == EntityModificationType.CREATE)
				throw new IllegalStateException("WTF?! How can an entity be created *after* it was changed?!");

			// A change does not matter anymore, if the object is deleted, anyway ;-)
			if (lastModification.getType() == EntityModificationType.CHANGE
					&& modification.getType() == EntityModificationType.DELETE) {
				modifications.remove(lastModification);
				entityId2LastModification.remove(entityId);
				// *not* return, because the deletion must be registered!
			}

			// CREATE + DELETE means the final state *after* the transaction is: *NOT* changed at all!
			// Note: CREATE + CHANGE + DELETE ends up here, too, because the CHANGE following the CREATE is
			// ignored above (keeping lastModification = CREATE).
			if (lastModification.getType() == EntityModificationType.CREATE
					&& modification.getType() == EntityModificationType.DELETE) {
				modifications.remove(lastModification);
				entityId2LastModification.remove(entityId);
				return;
			}
		}

		modifications.add(modification);
		entityId2LastModification.put(entityId, modification);
	}

	private EntityId getEntityId(final Object entity) {
		requireNonNull(entity, "entity");

		if (entity instanceof Entity) {
			final long id = ((Entity) entity).getId();
			return new EntityId(entity.getClass(), id);
		}

		logger.warn("getEntityId: entity is not an instance of Entity! class={} id={}",
				entity.getClass().getName(), JDOHelper.getObjectId(entity));

		return null;
	}
}
