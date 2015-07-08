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
	private boolean identityWizardNeeded;

	private ServerWizard serverWizard;
	private volatile boolean serverWizardNeeded;
	private volatile boolean serverWizardCompleted;

	public Welcome(Window owner) {
		this.owner = assertNotNull("owner", owner);
	}

	public boolean welcome() {
		final boolean[] result = new boolean[1];

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
						Platform.runLater(() -> {
							WizardDialog dialog = new WizardDialog(owner, serverWizard);
							dialog.show();
						});
					}
				});
			}
		});

		PlatformUtil.runAndWait(() -> {
			if (identityWizardNeeded) {
				WizardDialog dialog = new WizardDialog(owner, identityWizard);
				dialog.showAndWait();
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

			if (serverWizardNeeded)
				result[0] &= WizardState.FINISHED == serverWizard.getState();
		});

		return result[0];
	}

	private void determineServerWizardCompleted() {
		PlatformUtil.runAndWait(() -> {
			serverWizardCompleted = WizardState.CANCELLED == serverWizard.getState()
					|| WizardState.FINISHED == serverWizard.getState();
		});
	}
}
