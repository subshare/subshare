package org.subshare.gui.welcome;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.PgpPrivateKeyPassphraseManagerLs;
import org.subshare.gui.ls.ServerRegistryLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.pgp.createkey.advanced.AdvancedWizardPage;
import org.subshare.gui.pgp.createkey.passphrase.PassphraseWizardPage;
import org.subshare.gui.pgp.createkey.validity.ValidityWizardPage;
import org.subshare.gui.util.PlatformUtil;
import org.subshare.gui.welcome.createpgpkey.identity.IdentityWizardPage;
import org.subshare.gui.welcome.first.FirstWizardPage;
import org.subshare.gui.welcome.importbackup.ImportBackupWizardPage;
import org.subshare.gui.wizard.DefaultFinishingPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class WelcomeWizard extends Wizard {

	private final WelcomeData welcomeData;

	private final Pgp pgp;
	private boolean needToCreateIdentity;
	private boolean needToRegisterServer;

	private User user;

	public WelcomeWizard(final WelcomeData welcomeData) {
		this.welcomeData = assertNotNull("welcomeData", welcomeData); //$NON-NLS-1$

		pages.add(new FirstWizardPage());

		pgp = PgpLs.getPgpOrFail();
		if (pgp.getMasterKeysWithPrivateKey().isEmpty()) {
			needToCreateIdentity = true;
			pages.addAll(
					new IdentityWizardPage(welcomeData),
					new ImportBackupWizardPage(welcomeData),
					new PassphraseWizardPage(welcomeData.getCreatePgpKeyParam()),
					new ValidityWizardPage(welcomeData.getCreatePgpKeyParam()),
					new AdvancedWizardPage(welcomeData.getCreatePgpKeyParam())
					);
		}

		ServerRegistry serverRegistry = ServerRegistryLs.getServerRegistry();
		if (serverRegistry.getServers().isEmpty()) {
			needToRegisterServer = true;
//			pages.add(new ServerPage(welcomeData));
		}
	}

	public WelcomeData getWelcomeData() {
		return welcomeData;
	}

	@Override
	public String getTitle() {
		return Messages.getString("WelcomeWizard.title"); //$NON-NLS-1$
	}

	public boolean isNeeded() {
		return needToCreateIdentity || needToRegisterServer;
	}

	@Override
	protected void preFinish() {

		welcomeData.getCreatePgpKeyParam().getUserIds().removeIf(pgpUserId -> pgpUserId.isEmpty());

	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		if (needToCreateIdentity) {
			setHeaderText(String.format(Messages.getString("WelcomeWizard.statusMessage[creatingPgpKey]"), welcomeData.getPgpUserId())); //$NON-NLS-1$

			if (user == null)
				createUser();

			createPgpKey();
			needToCreateIdentity = false;
		}

		if (needToRegisterServer) {
			setHeaderText(Messages.getString("WelcomeWizard.statusMessage[registeringServer]"));

			// TODO Auto-generated method stub
			try {
				Thread.sleep(20000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void createUser() {
		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();

		user = userRegistry.createUser();
		user.setFirstName(welcomeData.firstNameProperty().get());
		user.setLastName(welcomeData.lastNameProperty().get());

		String email = welcomeData.getPgpUserId().getEmail();
		if (! isEmpty(email))
			user.getEmails().add(email);

		userRegistry.addUser(user);
	}

	private void createPgpKey() {
		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore();
		final PgpKey pgpKey = pgp.createPgpKey(welcomeData.getCreatePgpKeyParam());
		user.getPgpKeyIds().add(pgpKey.getPgpKeyId());
		pgpPrivateKeyPassphraseStore.putPassphrase(pgpKey.getPgpKeyId(), welcomeData.getCreatePgpKeyParam().getPassphrase());
	}

	private void setHeaderText(final String text) {
		PlatformUtil.runAndWait(new Runnable() {
			@Override
			public void run() {
				((DefaultFinishingPage) getFinishingPage()).getHeaderText().setText(text);
			}
		});
	}
}
