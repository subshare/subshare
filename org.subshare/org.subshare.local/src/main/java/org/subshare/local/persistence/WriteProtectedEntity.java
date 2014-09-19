package org.subshare.local.persistence;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.sign.Signable;

public interface WriteProtectedEntity extends Signable {

	PermissionType getPermissionTypeRequiredForWrite();

}
