package org.subshare.gui.welcome;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.subshare.core.observable.ObservableList;
import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.PgpUserId;
import org.subshare.gui.pgp.createkey.FxPgpUserId;
import org.subshare.gui.pgp.createkey.TimeUnit;

public class WelcomeData {

	private final CreatePgpKeyParam createPgpKeyParam = new CreatePgpKeyParam();
	private final FxPgpUserId pgpUserId = new FxPgpUserId();
	private final BooleanProperty importBackupProperty = new SimpleBooleanProperty(this, "importBackup");

	{
		createPgpKeyParam.setValiditySeconds(TimeUnit.YEAR.getSeconds() * 10);
		getPgpUserId();
	}

	public WelcomeData() {
	}

	public CreatePgpKeyParam getCreatePgpKeyParam() {
		return createPgpKeyParam;
	}

	public FxPgpUserId getPgpUserId() {
		ObservableList<PgpUserId> userIds = createPgpKeyParam.getUserIds();
		if (userIds.isEmpty() || userIds.get(0) != pgpUserId)
			userIds.add(0, new FxPgpUserId());

		return pgpUserId;
	}

	public BooleanProperty importBackupProperty() {
		return importBackupProperty;
	}
}
