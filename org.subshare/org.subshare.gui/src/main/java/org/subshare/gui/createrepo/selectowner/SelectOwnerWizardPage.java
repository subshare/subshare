package org.subshare.gui.createrepo.selectowner;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javafx.scene.Parent;
import javafx.scene.control.SelectionMode;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.ls.PgpPrivateKeyPassphraseManagerLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.selectuser.SelectUserPane;
import org.subshare.gui.wizard.WizardPage;

public class SelectOwnerWizardPage extends WizardPage {

	private final CreateRepoData createRepoData;
	private SelectUserPane selectUserPane;
	private List<User> usersWithUnlockedPgpKeys;

	public SelectOwnerWizardPage(final CreateRepoData createRepoData) {
		super(Messages.getString("SelectOwnerWizardPage.title")); //$NON-NLS-1$
		this.createRepoData = assertNotNull("createRepoData", createRepoData); //$NON-NLS-1$
	}

	@Override
	protected void init() {
		super.init();
		// TODO set the next page dependent on whether the selected user has multiple active OpenPGP keys! In this case, the user must select a specific OpenPGP key!!!
//		setNextPage(new IssueInvitationDestWizardPage(issueInvitationData));
	}

	public boolean isNeeded() {
		return getUsersWithUnlockedPgpKeys().size() != 1;
	}

	protected synchronized List<User> getUsersWithUnlockedPgpKeys() {
		if (usersWithUnlockedPgpKeys == null) {
			final Set<PgpKeyId> pgpKeyIds = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore().getPgpKeyIdsHavingPassphrase();
			if (pgpKeyIds.isEmpty())
				throw new IllegalStateException("There is no PGP private key unlocked."); // TODO show nice message and ask the user, if he would like to unlock.

			final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();
			final Collection<User> users = userRegistry.getUsersByPgpKeyIds(pgpKeyIds);

			if (users.isEmpty())
				throw new IllegalStateException("There is no user for any of these PGP keys: " + pgpKeyIds); // TODO should we ask to unlock (further) PGP keys?

			usersWithUnlockedPgpKeys = new ArrayList<>(users);
		}
		return usersWithUnlockedPgpKeys;
	}

	@Override
	protected Parent createContent() {
		selectUserPane = new SelectUserPane(
				getUsersWithUnlockedPgpKeys(), createRepoData.getOwners(), SelectionMode.SINGLE,
				Messages.getString("SelectOwnerWizardPage.selectUserPane.headerText")) { //$NON-NLS-1$
			@Override
			protected void updateComplete() {
				SelectOwnerWizardPage.this.setComplete(! createRepoData.getOwners().isEmpty());
			}
		};
		return selectUserPane;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (selectUserPane != null)
			selectUserPane.requestFocus();
	}
}
