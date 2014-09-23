package org.subshare.core.dto;

import org.subshare.core.sign.Signable;

public interface SsRepoFileDto extends Signable {

	String getName();
	void setName(String name);

	String getParentName();
	void setParentName(final String parentName);

}
