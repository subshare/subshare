package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

public class PgpKeyTreeItem<T> extends TreeItem<PgpKeyTreeItem<?>> {

//	private static final EventType<?> VALUE_OBJECT_CHANGED_EVENT = new EventType<>(treeNotificationEvent(), "PgpKeyTreeItemValueObjectChangedEvent");

//	private final ObjectProperty<T> valueObject;

//	private final PropertyChangeListener valueObjectPropertyChangeListener = new PropertyChangeListener() {
//		@Override
//		public void propertyChange(PropertyChangeEvent evt) {
//			setValue(getValueString());
//		}
//	};

//	public PgpKeyTreeItem() {
//		this(null);
//	}

	private T valueObject;

	public PgpKeyTreeItem(T valueObject) {
		this(valueObject, null);
	}

	public PgpKeyTreeItem(T valueObject, Node graphic) {
//		valueObjectProperty().addListener(new ChangeListener<T>() {
//			@Override
//			public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
//				setValue(getValueString());
//			}
//		});
//
//		setValueObject(valueObject);
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

//	protected String getValueString() {
//		final T valueObject = getValueObject();
//		return valueObject == null ? "" : valueObject.toString();
//	}

//	public T getValueObject() {
//		return valueObjectProperty().get();
//	}
//
//	public void setValueObject(T valueObject) {
//		valueObjectProperty().set(valueObject);
//	}

//	public final ObjectProperty<T> valueObjectProperty() {
//		if (valueObject == null) {
//			valueObject = new ObjectPropertyBase<T>() {
//				@Override public void set(T newValue) {
//					final T old = get();
//					if (old != null && old != newValue)
//						unhookPropertyChangeListener(old);
//
//					super.set(newValue);
//
//					if (newValue != null && old != newValue)
//						hookPropertyChangeListener(newValue);
//				}
//
//				@Override protected void invalidated() {
//					fireEvent(new MainTreeItemEvent(VALUE_OBJECT_CHANGED_EVENT, PgpKeyTreeItem.this));
//				}
//
//				@Override public Object getBean() {
//					return PgpKeyTreeItem.this;
//				}
//
//				@Override public String getName() {
//					return "valueObject";
//				}
//			};
//		}
//		return valueObject;
//	}

//	private void hookPropertyChangeListener(final Object object) {
//		try {
//			ReflectionUtil.invoke(object, "addPropertyChangeListener",
//					new Class<?>[] { PropertyChangeListener.class },
//					valueObjectPropertyChangeListener);
//		} catch (RuntimeException x) {
//			if (ExceptionUtil.getCause(x, NoSuchMethodException.class) == null)
//				throw x;
//		}
//	}
//
//	private void unhookPropertyChangeListener(final Object object) {
//		try {
//			ReflectionUtil.invoke(object, "removePropertyChangeListener",
//					new Class<?>[] { PropertyChangeListener.class },
//					valueObjectPropertyChangeListener);
//		} catch (RuntimeException x) {
//			if (ExceptionUtil.getCause(x, NoSuchMethodException.class) == null)
//				throw x;
//		}
//	}

	public static class MainTreeItemEvent extends Event {
		private static final long serialVersionUID = 1L;

		private final PgpKeyTreeItem<?> mainTreeItem;

		public MainTreeItemEvent(EventType<? extends Event> eventType, PgpKeyTreeItem<?> mainTreeItem) {
			super(eventType);
			this.mainTreeItem = mainTreeItem;
		}

		public PgpKeyTreeItem<?> getMainTreeItem() {
			return mainTreeItem;
		}
	}

//	private void fireEvent(MainTreeItemEvent evt) {
//		Event.fireEvent(this, evt);
//	}

	protected TreeTableView<String> getTreeTableView() {
		final PgpKeyTreeItem<?> parent = (PgpKeyTreeItem<?>) getParent();
		assertNotNull("parent", parent);
		return parent.getTreeTableView();
	}
}
