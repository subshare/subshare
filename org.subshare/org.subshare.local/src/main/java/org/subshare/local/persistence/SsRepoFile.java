package org.subshare.local.persistence;

import org.subshare.core.sign.Signature;

public interface SsRepoFile {

	Signature getSignature();

	void setSignature(Signature signature);

}
