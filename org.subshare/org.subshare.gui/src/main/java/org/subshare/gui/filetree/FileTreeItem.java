package org.subshare.gui.filetree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.List;

import co.codewizards.cloudstore.core.oio.File;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;

public class FileTreeItem<T> extends TreeItem<FileTreeItem<?>> {

	private T valueObject;

	private boolean childrenLoaded;

	private FileTreePane fileTreePane;

	private final StringProperty sizeProperty = new SimpleStringProperty(this, "size");
	private final StringProperty lastModifiedProperty = new SimpleStringProperty(this, "lastModified");

	private final RefreshListener refreshListener = event -> refresh();

	public FileTreeItem(T valueObject) {
		this(valueObject, null);

		parentProperty().addListener((ChangeListener<TreeItem<FileTreeItem<?>>>) (observable, oldValue, newValue) -> {
			if (newValue != null)
				fileTreePane = newValue.getValue().getFileTreePane();

			final FileTreePane oldFileTreePane = oldValue == null ? null : oldValue.getValue().getFileTreePane();
			final FileTreePane newFileTreePane = newValue == null ? null : newValue.getValue().getFileTreePane();
			if (oldFileTreePane != newFileTreePane) {
				if (oldFileTreePane != null)
					oldFileTreePane.removeRefreshListener(refreshListener);

				if (newFileTreePane != null) {
					newFileTreePane.addRefreshListener(refreshListener);
					refresh();
				}
			}
		});
	}

	protected void refresh() {
	}

	public FileTreeItem(T valueObject, Node graphic) {
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

	public StringProperty sizeProperty() {
		return sizeProperty;
	}

	public StringProperty lastModifiedProperty() {
		return lastModifiedProperty;
	}

	protected FileTreePane getFileTreePane() {
		if (fileTreePane != null)
			return fileTreePane;

		final FileTreeItem<?> parent = (FileTreeItem<?>) getParent();
		assertNotNull("parent", parent);
		return parent.getFileTreePane();
	}

	@Override
	public ObservableList<TreeItem<FileTreeItem<?>>> getChildren() {
		final ObservableList<TreeItem<FileTreeItem<?>>> children = super.getChildren();
		if (!childrenLoaded) {
			childrenLoaded = true; // *must* be set before clear()/addAll(...), because of events being fired.
			final List<FileTreeItem<?>> c = loadChildren();

			if (!children.isEmpty())
				children.clear();

			if (c != null)
				children.addAll(c);
		}
		return children;
	}

	protected boolean isChildrenLoaded() {
		return childrenLoaded;
	}

	protected void reloadChildren() {
		if (childrenLoaded)
			childrenLoaded = false;

		getChildren(); // implicitly (re)loads.
	}

	protected List<FileTreeItem<?>> loadChildren() {
		return null;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getName(), valueObject);
	}

	public FileTreeItem<?> findFirst(final File file) {
		assertNotNull("file", file);

		if (! file.isAbsolute())
			throw new IllegalArgumentException("file not absolute!");

		// TODO we should avoid loading all children! we should check by a path-test whether
		// this is actually possible before we dive into the children. strange though - I thought
		// I had already done this... YES, it is implemented in FileFileTreeItem!
		for (final TreeItem<FileTreeItem<?>> child : getChildren()) {
			final FileTreeItem<?> treeItem = child.getValue().findFirst(file);
			if (treeItem != null)
				return treeItem;
		}
		return null;
	}

	public List<FileTreeItem<?>> findAll(final File file) {
		assertNotNull("file", file);

		if (! file.isAbsolute())
			throw new IllegalArgumentException("file not absolute!");

		final List<FileTreeItem<?>> result = new ArrayList<FileTreeItem<?>>();
		for (final TreeItem<FileTreeItem<?>> child : getChildren()) {
			final List<FileTreeItem<?>> childFound = child.getValue().findAll(file);
			result.addAll(assertNotNull("FileTreeItem.findAll(...)", childFound));
		}
		return result;
	}
}
