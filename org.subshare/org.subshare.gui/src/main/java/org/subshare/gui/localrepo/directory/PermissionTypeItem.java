package org.subshare.gui.localrepo.directory;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.PermissionType;

public class PermissionTypeItem {

	public static final PermissionTypeItem NONE = new PermissionTypeItem("_none_"); //$NON-NLS-1$
	public static final PermissionTypeItem MIXED = new PermissionTypeItem("_mixed_"); //$NON-NLS-1$

	private final PermissionType permissionType;
	private final String id;
	private final String text;

	public PermissionTypeItem(final PermissionType permissionType) {
		this.permissionType = assertNotNull("permissionType", permissionType);
		this.id = permissionType.name();
		this.text = getText();
	}

	public PermissionTypeItem(final String id) {
		this.permissionType = null;
		this.id = assertNotNull("id", id);
		this.text = getText();
	}

	private String getText() {
		return Messages.getString(String.format("PermissionTypeItem[%s].text", id));
	}

	public PermissionType getPermissionType() {
		return permissionType;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return text;
	}
}