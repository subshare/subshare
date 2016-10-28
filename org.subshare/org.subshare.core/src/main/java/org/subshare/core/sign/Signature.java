package org.subshare.core.sign;

import java.util.Date;

import co.codewizards.cloudstore.core.Uid;

public interface Signature {

	Date getSignatureCreated();

	Uid getSigningUserRepoKeyId();

	byte[] getSignatureData();

}
