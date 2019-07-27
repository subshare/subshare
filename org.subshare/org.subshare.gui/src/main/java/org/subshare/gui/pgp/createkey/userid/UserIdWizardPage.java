package org.subshare.gui.pgp.createkey.userid;

import static java.util.Objects.*;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.pgp.createkey.validity.ValidityWizardPage;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class UserIdWizardPage extends WizardPage {

	private final CreatePgpKeyParam createPgpKeyParam;

	public UserIdWizardPage(final CreatePgpKeyParam createPgpKeyParam) {
		super("Identities");
		this.createPgpKeyParam = requireNonNull(createPgpKeyParam, "createPgpKeyParam");
		setNextPage(new ValidityWizardPage(createPgpKeyParam));
	}

	@Override
	protected Parent createContent() {
		return new UserIdPane(createPgpKeyParam);
	}
}
