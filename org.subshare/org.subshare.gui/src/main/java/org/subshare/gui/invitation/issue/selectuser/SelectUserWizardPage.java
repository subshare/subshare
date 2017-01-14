package org.subshare.gui.invitation.issue.selectuser;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyValidity;
import org.subshare.core.user.User;
import org.subshare.gui.IconSize;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.invitation.issue.destination.IssueInvitationDestWizardPage;
import org.subshare.gui.invitation.issue.selectkey.SelectKeyWizardPage;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.selectuser.SelectUserPane;
import org.subshare.gui.severity.SeverityImageRegistry;
import org.subshare.gui.wizard.WizardPage;

import co.codewizards.cloudstore.core.Severity;
import javafx.collections.ObservableSet;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class SelectUserWizardPage extends WizardPage {

	private final IssueInvitationData issueInvitationData;
	private VBox contentVBox;
	private SelectUserPane selectUserPane;
	private Label statusLabel;
	private IssueInvitationDestWizardPage issueInvitationDestWizardPage;
	private SelectKeyWizardPage selectKeyWizardPage;
	private Pgp pgp;

	public SelectUserWizardPage(final IssueInvitationData issueInvitationData) {
		super(Messages.getString("SelectUserWizardPage.title")); //$NON-NLS-1$
		this.issueInvitationData = assertNotNull(issueInvitationData, "issueInvitationData"); //$NON-NLS-1$
	}

	@Override
	protected void init() {
		super.init();
		issueInvitationDestWizardPage = new IssueInvitationDestWizardPage(issueInvitationData);
		setNextPage(issueInvitationDestWizardPage);
	}

	@Override
	protected Parent createContent() {
		List<User> users = new ArrayList<>(UserRegistryLs.getUserRegistry().getUsers());
		contentVBox = new VBox(8);
		statusLabel = new Label();
		selectUserPane = new SelectUserPane(
				users, issueInvitationData.getInvitees(), SelectionMode.MULTIPLE,
				Messages.getString("SelectUserWizardPage.selectUserPane.headerText")) { //$NON-NLS-1$
			@Override
			protected void updateComplete() {
				determineComplete();
				if (SelectUserWizardPage.this.isComplete())
					determineNextPage();
			}
		};
		contentVBox.getChildren().add(0, selectUserPane);
		return contentVBox;
	}

	private void determineComplete() {
		final ObservableSet<User> invitees = issueInvitationData.getInvitees();
		boolean complete = true;
		Severity severity = null;
		statusLabel.setText(null);
		statusLabel.setTooltip(null);

		if (invitees.isEmpty()) {
			complete = false;
			severity = Severity.ERROR;
		}

		if (complete && isEmpty(statusLabel.getText())) {
			for (final User invitee : invitees) {
				if (invitee.getValidPgpKeys().isEmpty()) {
					complete = false;
					severity = Severity.ERROR;
					statusLabel.setText("At least one selected user does not have any valid PGP key!");
					statusLabel.setTooltip(new Tooltip("An invitation requires the invited user's PGP key. Either this user has no \nkey at all, or all his keys are unusable (disabled, revoked or expired).\n\nPlease ask this user to generate a new PGP key and send it to you."));
					break;
				}
			}
		}

		if (complete && isEmpty(statusLabel.getText())) {
			for (final User invitee : invitees) {
				for (final PgpKey pgpKey : invitee.getValidPgpKeys()) {
					final PgpKeyValidity validity = getPgp().getKeyValidity(pgpKey);
					if (validity.compareTo(PgpKeyValidity.MARGINAL) < 0) {
						severity = Severity.WARNING;
						statusLabel.setText("Selected user's PGP key is NOT TRUSTED!");
						statusLabel.setTooltip(new Tooltip("At least one of the PGP keys involved is NOT trusted. This means, you do not have \nany indication that the PGP key truly belongs to the user it claims to belong to."));
						break;
					}
				}
			}
		}

		if (complete && isEmpty(statusLabel.getText())) {
			for (final User invitee : invitees) {
				for (final PgpKey pgpKey : invitee.getValidPgpKeys()) {
					final PgpKeyValidity validity = getPgp().getKeyValidity(pgpKey);
					if (validity.compareTo(PgpKeyValidity.FULL) < 0) {
						severity = Severity.INFO;
						statusLabel.setText("Selected user's PGP key is not fully trusted (only marginally)!");
						statusLabel.setTooltip(new Tooltip("At least one of the PGP keys involved is only marginally trusted. This means, there is \na low risk that the PGP key does not truly belong to the user it claims to belong to."));
						break;
					}
				}
			}
		}

		if (isEmpty(statusLabel.getText())) {
			statusLabel.setGraphic(null);
			contentVBox.getChildren().remove(statusLabel);
		}
		else {
			statusLabel.setGraphic(severity == null
					? null : new ImageView(SeverityImageRegistry.getInstance().getImage(severity, IconSize._24x24)));

			if (! contentVBox.getChildren().contains(statusLabel))
				contentVBox.getChildren().add(statusLabel);
		}
		setComplete(complete);
	}

	private void determineNextPage() {
		WizardPage nextPage = issueInvitationDestWizardPage;
		iterateUsers: for (final User invitee : issueInvitationData.getInvitees()) {
			final Set<PgpKey> validPgpKeys = invitee.getValidPgpKeys();
			final Set<PgpKeyValidity> validities = new HashSet<>(validPgpKeys.size());
			for (final PgpKey pgpKey : validPgpKeys) {
				final PgpKeyValidity validity = getPgp().getKeyValidity(pgpKey);
				if (validity.compareTo(PgpKeyValidity.FULL) < 0) {
					nextPage = getSelectKeyWizardPage();
					break iterateUsers;
				}
				validities.add(validity);
			}

			if (validities.size() > 1) {
				nextPage = getSelectKeyWizardPage();
				break iterateUsers;
			}
		}
		// TODO implement the SelectKeyWizardPage and assign it here!
//		setNextPage(nextPage);
	}

	protected SelectKeyWizardPage getSelectKeyWizardPage() {
		if (selectKeyWizardPage == null)
			selectKeyWizardPage = new SelectKeyWizardPage(issueInvitationData);

		return selectKeyWizardPage;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (selectUserPane != null)
			selectUserPane.requestFocus();
	}

	protected Pgp getPgp() {
		if (pgp == null)
			pgp = PgpLs.getPgpOrFail();

		return pgp;
	}
}
