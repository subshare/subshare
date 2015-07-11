package org.subshare.gui.welcome;

import static co.codewizards.cloudstore.core.util.StringUtil.*;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.PgpPrivateKeyPassphraseManagerLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.pgp.createkey.advanced.AdvancedWizardPage;
import org.subshare.gui.pgp.createkey.passphrase.PassphraseWizardPage;
import org.subshare.gui.pgp.createkey.validity.ValidityWizardPage;
import org.subshare.gui.welcome.first.FirstWizardPage;
import org.subshare.gui.welcome.identity.IdentityWizardPage;
import org.subshare.gui.welcome.importbackup.ImportBackupWizardPage;
import org.subshare.gui.wizard.DefaultFinishingPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class IdentityWizard extends Wizard {

	private final IdentityData identityData = new IdentityData();

	private final Pgp pgp;
	private boolean needed;

	private User user;

	public IdentityWizard() {
		pages.add(new FirstWizardPage());

		pgp = PgpLs.getPgpOrFail();
		if (pgp.getMasterKeysWithPrivateKey().isEmpty()) {
			needed = true;
			pages.addAll(
					new IdentityWizardPage(identityData),
					new ImportBackupWizardPage(identityData),
					new PassphraseWizardPage(identityData.getCreatePgpKeyParam()),
					new ValidityWizardPage(identityData.getCreatePgpKeyParam()),
					new AdvancedWizardPage(identityData.getCreatePgpKeyParam())
					);
		}
	}

	public IdentityData getIdentityData() {
		return identityData;
	}

	@Override
	public String getTitle() {
		return Messages.getString("IdentityWizard.title"); //$NON-NLS-1$
	}

	public boolean isNeeded() {
		return needed;
	}

	@Override
	protected void finishing() {
		identityData.getCreatePgpKeyParam().getUserIds().removeIf(pgpUserId -> pgpUserId.isEmpty());
		((DefaultFinishingPage) getFinishingPage()).getHeaderText().setText(
				String.format(Messages.getString("IdentityWizard.finishingPage.headerText.text"), identityData.getPgpUserId())); //$NON-NLS-1$
		super.finishing();
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
//		PlatformUtil.runAndWait(() -> ((DefaultFinishingPage) getFinishingPage()).getHeaderText().setText(
//				String.format(Messages.getString("IdentityWizard.finishingPage.headerText.text"), identityData.getPgpUserId()))); //$NON-NLS-1$

		createUserIfNeeded();
		createPgpKey();
	}

	private void createUserIfNeeded() {
		if (this.user != null)
			return;

		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();

		User user = userRegistry.createUser();
		user.setFirstName(identityData.firstNameProperty().get());
		user.setLastName(identityData.lastNameProperty().get());

		String email = identityData.getPgpUserId().getEmail();
		if (! isEmpty(email))
			user.getEmails().add(email);

		userRegistry.addUser(user);
		this.user = user;
	}

	private void createPgpKey() {
		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore();
		final PgpKey pgpKey = pgp.createPgpKey(identityData.getCreatePgpKeyParam());
		user.getPgpKeyIds().add(pgpKey.getPgpKeyId());
		pgpPrivateKeyPassphraseStore.putPassphrase(pgpKey.getPgpKeyId(), identityData.getCreatePgpKeyParam().getPassphrase());
	}
}
