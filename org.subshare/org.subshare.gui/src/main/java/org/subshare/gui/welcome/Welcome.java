package org.subshare.gui.welcome;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.stage.Window;

import org.subshare.gui.util.PlatformUtil;
import org.subshare.gui.wizard.WizardDialog;
import org.subshare.gui.wizard.WizardState;

public class Welcome {

	private final Window owner;

	private IdentityWizard identityWizard;
	private volatile boolean identityWizardNeeded;

	private ServerWizard serverWizard;
	private volatile boolean serverWizardNeeded;
	private volatile boolean serverWizardCompleted;

	public Welcome(Window owner) {
		this.owner = assertNotNull("owner", owner);
	}

	private void init() {
		PlatformUtil.runAndWait(() -> {
			identityWizard = new IdentityWizard();
			identityWizardNeeded = identityWizard.isNeeded();

			// We do not show the first page twice => suppress if already shown by IdentityWizard.
			boolean showFirstPage = ! identityWizardNeeded;

			// We only sync when we already have an identity - otherwise this is not necessary, since
			// a newly created identity cannot yet have a locker.
			boolean syncLocker = ! identityWizardNeeded;
			serverWizard = new ServerWizard(showFirstPage, syncLocker);
			serverWizardNeeded = serverWizard.isNeeded();

			// If we need both, identity-creation and server-registration, we start the 2nd wizard already
			// while the first is still running. PGP key generation can take *very* long and it is not needed
			// to wait.
			if (identityWizardNeeded && serverWizardNeeded) {
				identityWizard.stateProperty().addListener((InvalidationListener) observable -> {
					if (WizardState.FINISHING == identityWizard.getState()) {
						if (identityWizard.getIdentityData().importBackupProperty().get()) {
							// The backup contains the ServerRegistry. Hence, we do not need the ServerWizard.
							serverWizardNeeded = false;
							return;
						}

						Platform.runLater(() -> {
							WizardDialog dialog = new WizardDialog(owner, serverWizard);
							dialog.show();
						});
					}
				});
			}
		});
	}

	public boolean welcome() {
		init();

		final boolean[] result = new boolean[1];

		PlatformUtil.runAndWait(() -> {
			if (identityWizardNeeded) {
				WizardDialog dialog = new WizardDialog(owner, identityWizard);
				dialog.showAndWait();

				if (WizardState.FINISHED != identityWizard.getState())
					serverWizardNeeded = false; // if identityWizard was cancelled, we do not wait for the serverWizard, which was probably not even started!

				determineServerWizardCompleted();
			}
			else if (serverWizardNeeded) {
				WizardDialog dialog = new WizardDialog(owner, serverWizard);
				dialog.showAndWait();
			}
		});

		if (identityWizardNeeded && serverWizardNeeded) {
			// Since they run/ran in parallel and the ServerWizard is started with "runLater(...)",
			// we must wait for it, now. This way of waiting is not nice, but it's acceptable at least for now ;-)
			while (!serverWizardCompleted) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return false; // simply leaving when interrupted.
				}
				determineServerWizardCompleted();
			}
		}

		PlatformUtil.runAndWait(() -> {
			if (identityWizardNeeded)
				result[0] = WizardState.FINISHED == identityWizard.getState();
			else
				result[0] = true;

			// It should be OK to cancel the serverWizard, because (1) we can add the server later,
			// (2) maybe we do not want to add a server at all, because we want to instead export
			// our public key (maybe just created) and wait for an invitation token. This workflow
			// is not covered by the wizards - and maybe it does not need to be.
//			if (serverWizardNeeded)
//				result[0] &= WizardState.FINISHED == serverWizard.getState();
		});

		return result[0];
	}

	private void determineServerWizardCompleted() {
		if (!serverWizardNeeded)
			return;

		PlatformUtil.runAndWait(() -> {
			serverWizardCompleted = WizardState.CANCELLED == serverWizard.getState()
					|| WizardState.FINISHED == serverWizard.getState();
		});
	}
}
