package org.subshare.gui.user;

import java.util.EventListener;

public interface EditUserListener extends EventListener {

	void onEdit(EditUserEvent event);

}
