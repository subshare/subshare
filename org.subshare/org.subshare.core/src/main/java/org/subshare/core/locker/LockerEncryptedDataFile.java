package org.subshare.core.locker;

import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.subshare.core.file.EncryptedDataFile;

import co.codewizards.cloudstore.core.Uid;

public class LockerEncryptedDataFile extends EncryptedDataFile {

	private static final String MANIFEST_PROPERTY_CONTENT_VERSION = "contentVersion";
	private static final String MANIFEST_PROPERTY_REPLACED_CONTENT_VERSIONS = "replacedContentVersions";
	private static final String MANIFEST_PROPERTY_CONTENT_NAME = "contentName";

	public LockerEncryptedDataFile(byte[] in) throws IOException {
		super(in);
	}

	public LockerEncryptedDataFile(InputStream in) throws IOException {
		super(in);
	}

	public LockerEncryptedDataFile() {
	}

	public Uid getContentVersion() {
		final String s = getManifestProperties().getProperty(MANIFEST_PROPERTY_CONTENT_VERSION);
		if (isEmpty(s))
			return null;
		else
			return new Uid(s);
	}

	public void setContentVersion(final Uid contentVersion) {
		if (contentVersion == null)
			getManifestProperties().remove(MANIFEST_PROPERTY_CONTENT_VERSION);
		else
			getManifestProperties().setProperty(MANIFEST_PROPERTY_CONTENT_VERSION, contentVersion.toString());
	}

	public List<Uid> getReplacedContentVersions() {
		final String s = getManifestProperties().getProperty(MANIFEST_PROPERTY_REPLACED_CONTENT_VERSIONS);
		if (isEmpty(s))
			return Collections.emptyList();

		final List<Uid> result = new ArrayList<Uid>(3);
		for (final String part : s.split(","))
			result.add(new Uid(part));

		return Collections.unmodifiableList(result);
	}

	public void setReplacedContentVersions(final Collection<Uid> replacedContentVersions) {
		if (replacedContentVersions == null || replacedContentVersions.isEmpty())
			getManifestProperties().remove(MANIFEST_PROPERTY_REPLACED_CONTENT_VERSIONS);
		else {
			final StringBuilder sb = new StringBuilder();
			for (final Uid replacedContentVersion : replacedContentVersions) {
				if (sb.length() > 0)
					sb.append(',');

				sb.append(replacedContentVersion);
			}
			getManifestProperties().setProperty(MANIFEST_PROPERTY_REPLACED_CONTENT_VERSIONS, sb.toString());
		}
	}

	public String getContentName() {
		return getManifestProperties().getProperty(MANIFEST_PROPERTY_CONTENT_NAME);
	}

	public void setContentName(final String contentName) {
		if (contentName == null)
			getManifestProperties().remove(MANIFEST_PROPERTY_CONTENT_NAME);
		else
			getManifestProperties().setProperty(MANIFEST_PROPERTY_CONTENT_NAME, contentName);
	}
}
