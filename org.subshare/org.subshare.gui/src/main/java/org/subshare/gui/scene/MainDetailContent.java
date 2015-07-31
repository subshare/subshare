package org.subshare.gui.scene;

import javafx.scene.Parent;

/**
 * Optional interface to be implemented by {@link Parent}-subclasses returned as
 * {@link org.subshare.gui.maintree.MainTreeItem#getMainDetailContent() mainDetailContent}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface MainDetailContent {
	/**
	 * Callback-method telling this UI control that it is now shown to the user.
	 */
	void onShown();

	/**
	 * Callback-method telling this UI control that it is not shown to the user, anymore.
	 */
	void onHidden();
}
