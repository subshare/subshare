package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.core.util.ReflectionUtil;

public class MainTreeItem<T> extends TreeItem<String> {

	private static final EventType<?> VALUE_OBJECT_CHANGED_EVENT = new EventType<>(treeNotificationEvent(), "ValueObjectChangedEvent");

	private ObjectProperty<T> valueObject;

	private final PropertyChangeListener valueObjectPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			setValue(getValueString());
		}
	};

	public MainTreeItem() {
		this(null);
	}

	public MainTreeItem(T valueObject) {
		this(valueObject, null);
	}

	public MainTreeItem(T valueObject, Node graphic) {
		valueObjectProperty().addListener(new ChangeListener<T>() {
			@Override
			public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
				setValue(getValueString());
			}
		});

		setValueObject(valueObject);
		setGraphic(graphic);
//		workaround_hookChangeListener();
	}

	protected String getValueString() {
		final T valueObject = getValueObject();
		return valueObject == null ? "" : valueObject.toString();
	}

	public T getValueObject() {
		return valueObjectProperty().get();
	}

	public void setValueObject(T valueObject) {
		valueObjectProperty().set(valueObject);
	}

	public final ObjectProperty<T> valueObjectProperty() {
		if (valueObject == null) {
			valueObject = new ObjectPropertyBase<T>() {
				@Override public void set(T newValue) {
					final T old = get();
					if (old != null && old != newValue)
						unhookPropertyChangeListener(old);

					super.set(newValue);

					if (newValue != null && old != newValue)
						hookPropertyChangeListener(newValue);
				}

				@Override protected void invalidated() {
					fireEvent(new MainTreeItemEvent(VALUE_OBJECT_CHANGED_EVENT, MainTreeItem.this));
				}

				@Override public Object getBean() {
					return MainTreeItem.this;
				}

				@Override public String getName() {
					return "valueObject";
				}
			};
		}
		return valueObject;
	}

	private void hookPropertyChangeListener(final Object object) {
		try {
			ReflectionUtil.invoke(object, "addPropertyChangeListener",
					new Class<?>[] { PropertyChangeListener.class },
					valueObjectPropertyChangeListener);
		} catch (RuntimeException x) {
			if (ExceptionUtil.getCause(x, NoSuchMethodException.class) == null)
				throw x;
		}
	}

	private void unhookPropertyChangeListener(final Object object) {
		try {
			ReflectionUtil.invoke(object, "removePropertyChangeListener",
					new Class<?>[] { PropertyChangeListener.class },
					valueObjectPropertyChangeListener);
		} catch (RuntimeException x) {
			if (ExceptionUtil.getCause(x, NoSuchMethodException.class) == null)
				throw x;
		}
	}

	public static class MainTreeItemEvent extends Event {
		private static final long serialVersionUID = 1L;

		private final MainTreeItem<?> mainTreeItem;

		public MainTreeItemEvent(EventType<? extends Event> eventType, MainTreeItem<?> mainTreeItem) {
			super(eventType);
			this.mainTreeItem = mainTreeItem;
		}

//		public MainTreeItemEvent(Object source, EventTarget target, EventType<? extends Event> eventType) {
//			super(source, target, eventType);
//		}

		public MainTreeItem<?> getMainTreeItem() {
			return mainTreeItem;
		}
	}

	private void fireEvent(MainTreeItemEvent evt) {
		Event.fireEvent(this, evt);
	}

	private Parent mainDetailContent;

	public Parent getMainDetailContent() {
		if (mainDetailContent == null) {
			mainDetailContent = createMainDetailContent();
		}
		return mainDetailContent;
	}

	protected Parent createMainDetailContent() {
		return null;
	}

	protected TreeView<String> getTreeView() {
		final MainTreeItem<?> parent = (MainTreeItem<?>) getParent();
		assertNotNull("parent", parent);
		return parent.getTreeView();
	}

//	/**
//	 * The treeView.selectedItems are not updated (or at least their ListChangeEvent is not fired) when a
//	 * tree item is removed. We therefore manually take care of the selection in this case.
//	 */
//	private void workaround_hookChangeListener() {
//		getChildren().addListener(workaroundChangeListener);
//	}
//
//	private ListChangeListener<? super TreeItem<String>> workaroundChangeListener = new ListChangeListener<TreeItem<String>>() {
//		@Override
//		public void onChanged(final ListChangeListener.Change<? extends TreeItem<String>> c) {
//			final List<TreeItem<String>> selectedItems = new ArrayList<>(getMainTree().getSelectionModel().getSelectedItems());
//
//			Set<TreeItem<String>> removedSet = null;
//
//			while (c.next()) {
//				if (c.getRemoved().isEmpty())
//					continue;
//
//				if (removedSet == null)
//					removedSet = new HashSet<>(c.getRemoved());
//				else
//					removedSet.addAll(c.getRemoved());
//			}
//
//			if (removedSet != null) {
//				boolean needsHelp = false;
//				for (final TreeItem<String> selectedItem : selectedItems) {
//					if (removedSet.contains(selectedItem))
//						needsHelp = true;
//				}
//
//				if (needsHelp) {
//					selectedItems.removeAll(removedSet);
//					getMainTree().getSelectionModel().clearSelection();
//					if (! selectedItems.isEmpty()) {
//						for (TreeItem<String> selectedItem : selectedItems)
//							getMainTree().getSelectionModel().select(selectedItem);
//					}
//				}
//			}
//		}
//	};
}
