package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javafx.scene.control.TreeView;

public class RootMainTreeItem extends MainTreeItem<String> {

	private final TreeView<String> mainTree;

//	/**
//	 * @deprecated Workaround for selection bug (treeView.selectedItems changes, but selection listener is not triggered).
//	 */
//	@Deprecated
//	private List<TreeItem<String>> lastSelectedItems = Collections.emptyList();
//
//	/**
//	 * @deprecated Workaround for selection bug (treeView.selectedItems changes, but selection listener is not triggered).
//	 */
//	@Deprecated
//	private final ListChangeListener<? super TreeItem<String>> mainTreeSelectionListener = new ListChangeListener<TreeItem<String>>() {
//		@Override
//		public void onChanged(final ListChangeListener.Change<? extends TreeItem<String>> c) {
//			while (c.next()) {
//				final ObservableList<? extends TreeItem<String>> selection = c.getList();
//				lastSelectedItems = Collections.unmodifiableList(new ArrayList<>(selection));
//			}
//		}
//	};

	public RootMainTreeItem(final TreeView<String> mainTree) {
		this.mainTree = assertNotNull(mainTree, "mainTree");
//		this.mainTree.getSelectionModel().getSelectedItems().addListener(mainTreeSelectionListener);
	}

	@Override
	protected TreeView<String> getTreeView() {
		return mainTree;
	}
}