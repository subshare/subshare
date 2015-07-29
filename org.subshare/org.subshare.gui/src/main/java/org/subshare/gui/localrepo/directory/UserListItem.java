package org.subshare.gui.localrepo.directory;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.User;

public class UserListItem extends org.subshare.gui.userlist.UserListItem {

	private final BooleanProperty owner = new SimpleBooleanProperty(this, "owner") { //$NON-NLS-1$
		@Override
		public void set(boolean newValue) {
			super.set(newValue);
			updateStrings();
		}
	};
	private final ObservableSet<PermissionType> effectivePermissionTypes = FXCollections.observableSet(new HashSet<PermissionType>());
	private final ObservableSet<PermissionType> grantedPermissionTypes = FXCollections.observableSet(new HashSet<PermissionType>());
	private final ObservableSet<PermissionType> inheritedPermissionTypes = FXCollections.observableSet(new HashSet<PermissionType>());
	private final InvalidationListener updateStringsInvalidationListener = observable -> updateStrings();

	private final StringProperty effectivePermissionString = new SimpleStringProperty(this, "effectivePermissionString"); //$NON-NLS-1$
	private final StringProperty grantedPermissionString = new SimpleStringProperty(this, "grantedPermissionString"); //$NON-NLS-1$
	private final StringProperty inheritedPermissionString = new SimpleStringProperty(this, "inheritedPermissionString"); //$NON-NLS-1$

	public UserListItem(User user) {
		super(user);
		effectivePermissionTypes.addListener(updateStringsInvalidationListener);
		grantedPermissionTypes.addListener(updateStringsInvalidationListener);
		inheritedPermissionTypes.addListener(updateStringsInvalidationListener);
	}

	private void updateStrings() {
		if (isOwner()) {
			final String ownerPermissionString = Messages.getString("UserListItem.ownerPermissionString"); //$NON-NLS-1$
			setEffectivePermissionString(ownerPermissionString);
			setGrantedPermissionString(ownerPermissionString);
			setInheritedPermissionString(ownerPermissionString);
		}
		else {
			setEffectivePermissionString(getPermissionString(effectivePermissionTypes));
			setGrantedPermissionString(getPermissionString(grantedPermissionTypes));
			setInheritedPermissionString(getPermissionString(inheritedPermissionTypes));
		}
	}

	private static String getPermissionString(final Set<PermissionType> permissionTypes) {
		assertNotNull("permissionTypes", permissionTypes); //$NON-NLS-1$
		final StringBuilder sb = new StringBuilder();

		final LinkedHashSet<PermissionType> allPermissionTypes = new LinkedHashSet<PermissionType>(
				Arrays.asList(PermissionType.grant, PermissionType.write, PermissionType.read));
		allPermissionTypes.addAll(Arrays.asList(PermissionType.values()));

		for (PermissionType permissionType : allPermissionTypes) {
			if (permissionTypes.contains(permissionType)) {
				if (sb.length() > 0)
					sb.append(Messages.getString("UserListItem.permissionTypeSeparator")); //$NON-NLS-1$

				sb.append(Messages.getString(String.format("UserListItem.permissionType[%s].text", permissionType.name()))); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	public final boolean isOwner() {
		return this.ownerProperty().get();
	}

	public final void setOwner(final boolean owner) {
		this.ownerProperty().set(owner);
	}

	public final BooleanProperty ownerProperty() {
		return this.owner;
	}

	public ObservableSet<PermissionType> getEffectivePermissionTypes() {
		return effectivePermissionTypes;
	}

	public ObservableSet<PermissionType> getGrantedPermissionTypes() {
		return grantedPermissionTypes;
	}

	public ObservableSet<PermissionType> getInheritedPermissionTypes() {
		return inheritedPermissionTypes;
	}

	public StringProperty effectivePermissionStringProperty() {
		return this.effectivePermissionString;
	}

	public String getEffectivePermissionString() {
		return this.effectivePermissionStringProperty().get();
	}

	public void setEffectivePermissionString(final String string) {
		this.effectivePermissionStringProperty().set(string);
	}

	public StringProperty grantedPermissionStringProperty() {
		return this.grantedPermissionString;
	}

	public String getGrantedPermissionString() {
		return this.grantedPermissionStringProperty().get();
	}

	public void setGrantedPermissionString(final String string) {
		this.grantedPermissionStringProperty().set(string);
	}

	public StringProperty inheritedPermissionStringProperty() {
		return this.inheritedPermissionString;
	}

	public String getInheritedPermissionString() {
		return this.inheritedPermissionStringProperty().get();
	}

	public void setInheritedPermissionString(final String string) {
		this.inheritedPermissionStringProperty().set(string);
	}

	public void copyFrom(UserListItem other) {
		this.setOwner(other.isOwner());

		this.effectivePermissionTypes.retainAll(other.effectivePermissionTypes);
		this.effectivePermissionTypes.addAll(other.effectivePermissionTypes);

		this.grantedPermissionTypes.retainAll(other.grantedPermissionTypes);
		this.grantedPermissionTypes.addAll(other.grantedPermissionTypes);

		this.inheritedPermissionTypes.retainAll(other.inheritedPermissionTypes);
		this.inheritedPermissionTypes.addAll(other.inheritedPermissionTypes);
	}
}
