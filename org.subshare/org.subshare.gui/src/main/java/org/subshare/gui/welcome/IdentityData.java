package org.subshare.gui.welcome;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.subshare.core.observable.ObservableList;
import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.PgpUserId;
import org.subshare.gui.pgp.createkey.FxPgpUserId;
import org.subshare.gui.pgp.createkey.TimeUnit;

public class IdentityData {

	private final CreatePgpKeyParam createPgpKeyParam = new CreatePgpKeyParam();

	private final StringProperty firstNameProperty = new SimpleStringProperty(this, "firstName");
	private final StringProperty lastNameProperty = new SimpleStringProperty(this, "lastName");

	private final FxPgpUserId pgpUserId = new FxPgpUserId();
	private final BooleanProperty importBackupProperty = new SimpleBooleanProperty(this, "importBackup");

	{
		createPgpKeyParam.setValiditySeconds(TimeUnit.YEAR.getSeconds() * 10);
		getPgpUserId();
		firstNameProperty.addListener((InvalidationListener) observable -> updatePgpUserIdName());
		lastNameProperty.addListener((InvalidationListener) observable -> updatePgpUserIdName());
	}

	public IdentityData() {
	}

	private void updatePgpUserIdName() {
		final String firstName = trim(firstNameProperty.get());
		final String lastName = trim(lastNameProperty.get());

		final StringBuilder sb = new StringBuilder();
		if (! isEmpty(firstName))
			sb.append(firstName);

		if (! isEmpty(lastName)) {
			if (sb.length() > 0)
				sb.append(' ');

			sb.append(lastName);
		}

		getPgpUserId().setName(sb.toString());
	}

	public CreatePgpKeyParam getCreatePgpKeyParam() {
		return createPgpKeyParam;
	}

	public FxPgpUserId getPgpUserId() {
		ObservableList<PgpUserId> userIds = createPgpKeyParam.getUserIds();

		if (userIds.isEmpty() || userIds.get(0) != pgpUserId) {
			userIds.remove(pgpUserId);
			userIds.add(0, pgpUserId);
		}

		return pgpUserId;
	}

	public StringProperty firstNameProperty() {
		return firstNameProperty;
	}

	public StringProperty lastNameProperty() {
		return lastNameProperty;
	}

	public BooleanProperty importBackupProperty() {
		return importBackupProperty;
	}
}
