package org.subshare.gui.filetree;

import java.util.EventListener;
import java.util.EventObject;

public interface RefreshListener extends EventListener {

	public static class RefreshEvent extends EventObject {
		private static final long serialVersionUID = 1L;

		public RefreshEvent(Object source) {
			super(source);
		}
	}

	void onRefresh(RefreshEvent event);
}
