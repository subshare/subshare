package org.subshare.gui.pgp.keytree;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.subshare.core.pgp.Pgp;
import org.subshare.gui.ls.PgpLs;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

public class PgpKeyTreeItem<T> extends TreeItem<PgpKeyTreeItem<?>> {

	private T valueObject;

	private Pgp pgp;

	private final BooleanProperty checked = new SimpleBooleanProperty(this, "checked") {
		@Override
		public void set(final boolean newValue) {
			super.set(newValue);

			if (newValue)
				getPgpKeyTreePane().getCheckedTreeItems().add(PgpKeyTreeItem.this);
			else
				getPgpKeyTreePane().getCheckedTreeItems().remove(PgpKeyTreeItem.this);
		}
	};

	private final PropertyChangeListener trustDbPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			updateKeyValidityAndOwnerTrust();
		}
	};

	private boolean trustDbPropertyChangeListenerHooked;

	private final StringProperty keyValidity = new SimpleStringProperty(this, "keyValidity");
	private final StringProperty ownerTrust = new SimpleStringProperty(this, "ownerTrust");

	public PgpKeyTreeItem(T valueObject) {
		this(valueObject, null);
	}

	public PgpKeyTreeItem(T valueObject, Node graphic) {
		setValue(this);
		this.valueObject = valueObject;
		setGraphic(graphic);
	}

	protected void updateKeyValidityAndOwnerTrust() {
		keyValidity.set(getKeyValidity());
		ownerTrust.set(getOwnerTrust());
	}

	protected T getValueObject() {
		return valueObject;
	}

	public BooleanProperty checkedProperty() {
		return checked;
	}
	public boolean isChecked() {
		return checked.get();
	}
	public void setChecked(boolean checked) {
		this.checked.set(checked);
	}

	public String getName() {
		return getValueObject().toString();
	}

	public String getKeyId() {
		return null;
	}

	public String getKeyValidity() {
		return null;
	}

	public ReadOnlyStringProperty keyValidityProperty() {
		if (keyValidity.get() == null && getKeyValidity() != null) {
			hookTrustDbPropertyChangeListener();
			updateKeyValidityAndOwnerTrust();
		}
		return keyValidity;
	}

	public String getOwnerTrust() {
		return null;
	}

	public ReadOnlyStringProperty ownerTrustProperty() {
		if (ownerTrust.get() == null && getOwnerTrust() != null) {
			hookTrustDbPropertyChangeListener();
			updateKeyValidityAndOwnerTrust();
		}
		return ownerTrust;
	}

	public String getCreated() {
		return null;
	}

	public String getValidTo() {
		return null;
	};

	public String getAlgorithm() {
		return null;
	}

	public String getStrength() {
		return null;
	}

	public String getUsage() {
		return null;
	}

	protected TreeTableView<PgpKeyTreeItem<?>> getTreeTableView() {
		return getPgpKeyTreePane().getTreeTableView();
	}

	protected PgpKeyTreePane getPgpKeyTreePane() {
		final PgpKeyTreeItem<?> parent = (PgpKeyTreeItem<?>) getParent();
		assertNotNull("parent", parent);
		return parent.getPgpKeyTreePane();
	}

	public <I extends PgpKeyTreeItem<?>> I getThisOrParentPgpKeyTreeItemOfType(final Class<I> type) {
		assertNotNull("type", type);

		if (type.isInstance(this))
			return type.cast(this);

		final PgpKeyTreeItem<?> parent = (PgpKeyTreeItem<?>) getParent();
		if (parent == null)
			return null;

		return parent.getThisOrParentPgpKeyTreeItemOfType(type);
	}

	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getName(), valueObject);
	}

	protected Pgp getPgp() {
		if (pgp == null) {
			final PgpKeyTreeItem<?> parent = (PgpKeyTreeItem<?>) getParent();
			if (parent != null)
				pgp = parent.getPgp();
			else
				pgp = PgpLs.getPgpOrFail();
		}
		return pgp;
	}

	protected void hookTrustDbPropertyChangeListener() {
		if (! trustDbPropertyChangeListenerHooked) {
			trustDbPropertyChangeListenerHooked = true;
			addWeakPropertyChangeListener(getPgp(), Pgp.PropertyEnum.trustdb, trustDbPropertyChangeListener);
		}
	}
}
