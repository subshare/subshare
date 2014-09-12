package org.subshare.core.dto;

import org.subshare.core.sign.Signable;

public interface SsRepoFileDto extends Signable {

	String getParentName();
	void setParentName(final String parentName);

}
