package org.subshare.gui.pgp.keytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import org.subshare.core.pgp.Pgp;
import org.subshare.gui.ls.PgpLs;

public class PgpKeyTreeItem<T> extends TreeItem<PgpKeyTreeItem<?>> {

	private T valueObject;

	private Pgp pgp;

	public PgpKeyTreeItem(T valueObject) {
		this(valueObject, null);
	}

	public PgpKeyTreeItem(T valueObject, Node graphic) {
		setValue(this);
		this.valueObject = valueObject;
		setGraphic(graphic);
	}

	protected T getValueObject() {
		return valueObject;
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

	public String getOwnerTrust() {
		return null;
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

//	public static class MainTreeItemEvent extends Event {
//		private static final long serialVersionUID = 1L;
//
//		private final PgpKeyTreeItem<?> mainTreeItem;
//
//		public MainTreeItemEvent(EventType<? extends Event> eventType, PgpKeyTreeItem<?> mainTreeItem) {
//			super(eventType);
//			this.mainTreeItem = mainTreeItem;
//		}
//
//		public PgpKeyTreeItem<?> getMainTreeItem() {
//			return mainTreeItem;
//		}
//	}

	protected TreeTableView<PgpKeyTreeItem<?>> getTreeTableView() {
		final PgpKeyTreeItem<?> parent = (PgpKeyTreeItem<?>) getParent();
		assertNotNull("parent", parent);
		return parent.getTreeTableView();
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
		if (pgp == null)
			pgp = PgpLs.getPgpOrFail();

		return pgp;
	}
}
