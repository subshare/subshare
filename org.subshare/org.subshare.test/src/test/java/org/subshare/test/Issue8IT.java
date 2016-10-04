package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.DebugUserRepoKeyDto;
import org.subshare.core.dto.DebugUserRepoKeyDto.KeyRingType;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.core.user.UserRepoInvitationToken;

import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class Issue8IT extends AbstractMultiUserIT {
	private static final Logger logger = LoggerFactory.getLogger(Issue8IT.class);

	@Test
	public void issue_8_threesomeWithFreshmen() throws Exception {
		// ********************************************************************
		// *** Xenia (1st friend) ***
		// Xenia needs to create a new PGP key pair.
		switchLocationTo(TestUser.xenia);
		PgpKey pgpKey = createPgpKey();
		byte[] xeniaPgpKeyPublicData = getPgp().exportPublicKeys(Collections.singleton(pgpKey));
		pgpKey = null;


		// ********************************************************************
		// *** Yasmin (2nd friend) ***
		// Yasmin, too, needs to create a new PGP key pair.
		switchLocationTo(TestUser.yasmin);
		pgpKey = createPgpKey();
		byte[] yasminPgpKeyPublicData = getPgp().exportPublicKeys(Collections.singleton(pgpKey));
		pgpKey = null;


		// ********************************************************************
		// *** Marco's (OWNER) machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		syncPgp();
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncLocalWithRemoteRepo();

//		assertUserIdentityCountInRepoIs(localSrcRoot, 1);
//		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 1);
//		assertUserIdentityCountInRepoIs(remoteRoot, 1);
//		assertUserIdentityLinkCountInRepoIs(remoteRoot, 1);

		// Import Xenia's + Yasmin's public key into Owner's (Marco's) key ring.
		List<PgpKey> importedKeys = importKeys(xeniaPgpKeyPublicData);
		UserRegistryImpl.getInstance().importUsersFromPgpKeys(importedKeys);

		importedKeys = importKeys(yasminPgpKeyPublicData);
		UserRegistryImpl.getInstance().importUsersFromPgpKeys(importedKeys);

		// Create invitation token for Xenia.
		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.grant, TestUser.xenia);

		// Need to sync the data for the invitation-token! Otherwise the token is useless!
		syncLocalWithRemoteRepo();

		syncMetaOnlyRepos();

		syncPgp();

//		assertUserIdentitiesReadable(localSrcRoot);
//
//		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
//		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 4); // 2 user-identities readable by 2 users (2 * 2)
//		assertUserIdentityCountInRepoIs(remoteRoot, 2);
//		assertUserIdentityLinkCountInRepoIs(remoteRoot, 4);


		// ********************************************************************
		// *** Xenia ***
		switchLocationTo(TestUser.xenia);
		// create local repo, then connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo(userRepoInvitationToken);

		// and sync.
		syncLocalWithRemoteRepo();

		syncMetaOnlyRepos();


		// ********************************************************************
		// *** Marco (OWNER) ***
		switchLocationTo(TestUser.marco);

		syncLocalWithRemoteRepo();

		syncMetaOnlyRepos();


		// ********************************************************************
		// *** Xenia ***
		switchLocationTo(TestUser.xenia);

		// and sync.
		syncLocalWithRemoteRepo();

		syncMetaOnlyRepos();


//		assertUserIdentitiesReadable(localDestRoot);
//
//		assertUserIdentityCountInRepoIs(localDestRoot, 3); // 3 user-identities for 2 real keys + 1 invitation-key
//		assertUserIdentityLinkCountInRepoIs(localDestRoot, 8); // the 3 identities are readable by 2 real keys (3 * 2 = 6)
//		// + the invitation key can read itself => 7
//		// + lingering old data (for the invitation-key - this is cleaned up later)
//		assertUserIdentityCountInRepoIs(remoteRoot, 3);
//		assertUserIdentityLinkCountInRepoIs(remoteRoot, 8);


		// ********************************************************************
		// *** Marco (OWNER) ***
		switchLocationTo(TestUser.marco);

		// Create invitation token for Yasmin.
		userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.grant, TestUser.yasmin);

		syncLocalWithRemoteRepo();

		syncMetaOnlyRepos();


		// ********************************************************************
		// *** Yasmin ***
		switchLocationTo(TestUser.yasmin);

		// create local repo, then connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo(userRepoInvitationToken);

		// and sync.
		syncLocalWithRemoteRepo();

		syncMetaOnlyRepos();


		// ********************************************************************
		// *** Xenia ***
		switchLocationTo(TestUser.xenia);

		// Sync => https://github.com/subshare/subshare/issues/8
		syncLocalWithRemoteRepo();

		syncMetaOnlyRepos();


		Collection<DebugUserRepoKeyDto> debugUserRepoKeyDtos = getDebugUserRepoKeyDtos();

		assertThat(filterDebugUserRepoKeyDtos(
				debugUserRepoKeyDtos, false, KeyRingType.PAIR_CURRENT, TestUser.xenia.getEmail()))
		.hasSize(1);

		assertThat(filterDebugUserRepoKeyDtos(
				debugUserRepoKeyDtos, false, KeyRingType.PUBLIC, TestUser.marco.getEmail()))
		.hasSize(1);

		assertThat(filterDebugUserRepoKeyDtos(
				debugUserRepoKeyDtos, true, KeyRingType.PUBLIC, TestUser.yasmin.getEmail()))
		.hasSize(1);

		assertThat(filterDebugUserRepoKeyDtos(
				debugUserRepoKeyDtos, false, KeyRingType.NONE, TestUser.yasmin.getEmail()))
		.hasSize(1);

		assertThat(debugUserRepoKeyDtos).hasSize(4);


		syncPgp();
		syncLocalWithRemoteRepo();


		debugUserRepoKeyDtos = getDebugUserRepoKeyDtos();

		assertThat(filterDebugUserRepoKeyDtos(
				debugUserRepoKeyDtos, false, KeyRingType.PAIR_CURRENT, TestUser.xenia.getEmail()))
		.hasSize(1);

		assertThat(filterDebugUserRepoKeyDtos(
				debugUserRepoKeyDtos, false, KeyRingType.PUBLIC, TestUser.marco.getEmail()))
		.hasSize(1);

		assertThat(filterDebugUserRepoKeyDtos(
				debugUserRepoKeyDtos, true, KeyRingType.PUBLIC, TestUser.yasmin.getEmail()))
		.hasSize(1);

		assertThat(filterDebugUserRepoKeyDtos(
				debugUserRepoKeyDtos, false, KeyRingType.PUBLIC, TestUser.yasmin.getEmail()))
		.hasSize(1);

		assertThat(debugUserRepoKeyDtos).hasSize(4);
	}

	protected static List<DebugUserRepoKeyDto> filterDebugUserRepoKeyDtos(
			final Collection<DebugUserRepoKeyDto> debugUserRepoKeyDtos,
			final Boolean invitation,
			final KeyRingType keyRingType,
			final String email) {

		List<DebugUserRepoKeyDto> result = new ArrayList<>(debugUserRepoKeyDtos.size());
		for (DebugUserRepoKeyDto debugUserRepoKeyDto : debugUserRepoKeyDtos) {
			if (invitation != null && invitation.booleanValue() != debugUserRepoKeyDto.isInvitation())
				continue;

			if (keyRingType != null && keyRingType != debugUserRepoKeyDto.getKeyRingType())
				continue;

			if (email != null) {
				if (debugUserRepoKeyDto.getUserIdentityPayloadDto() == null)
					continue;

				if (! debugUserRepoKeyDto.getUserIdentityPayloadDto().getEmails().contains(email))
					continue;
			}

			result.add(debugUserRepoKeyDto);
		}
		return result;
	}

}
