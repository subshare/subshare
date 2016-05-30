package org.subshare.core.dto;

import java.util.Date;

import org.subshare.core.sign.Signable;

public interface SsRepoFileDto extends Signable {

	Date DUMMY_LAST_MODIFIED = new Date(0);

	String getName();
	void setName(String name);

	String getParentName();
	void setParentName(final String parentName);

}
