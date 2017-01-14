package org.subshare.core.file;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.core.file.FileConst.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;

public class DataFileFilter implements FileFilter {

	private static final Logger logger = LoggerFactory.getLogger(DataFileFilter.class);

	private boolean acceptDirectories = true;
	private String acceptContentType = null;
	private int acceptContentTypeVersionMin = 0;
	private int acceptContentTypeVersionMax = Integer.MAX_VALUE;

	public DataFileFilter() {
	}

	public boolean isAcceptDirectories() {
		return acceptDirectories;
	}
	public DataFileFilter setAcceptDirectories(boolean acceptDirectories) {
		this.acceptDirectories = acceptDirectories;
		return this;
	}

	public String getAcceptContentType() {
		return acceptContentType;
	}
	public DataFileFilter setAcceptContentType(String acceptContentType) {
		this.acceptContentType = acceptContentType;
		return this;
	}

	public int getAcceptContentTypeVersionMin() {
		return acceptContentTypeVersionMin;
	}
	public DataFileFilter setAcceptContentTypeVersionMin(int acceptContentTypeVersionMin) {
		this.acceptContentTypeVersionMin = acceptContentTypeVersionMin;
		return this;
	}
	public int getAcceptContentTypeVersionMax() {
		return acceptContentTypeVersionMax;
	}
	public DataFileFilter setAcceptContentTypeVersionMax(int acceptContentTypeVersionMax) {
		this.acceptContentTypeVersionMax = acceptContentTypeVersionMax;
		return this;
	}

	@Override
	public boolean accept(final File file) {
		assertNotNull(file, "file");
		if (isAcceptDirectories() && file.isDirectory())
			return true;

		if (! acceptFileName(file))
			return false;

		return acceptContentType(file);
	}

	protected boolean acceptFileName(File file) {
		return file.getName().endsWith(SUBSHARE_FILE_EXTENSION);
	}

	protected boolean acceptContentType(File file) {
		assertNotNull(file, "file");
		try {
			final DataFile dataFile;
			try (final InputStream in = castStream(file.createInputStream())) {
				dataFile = new DataFile(in) {
					@Override protected String getContentTypeValue() {
						// not needed as we neither write nor check the content-type (all are accepted).
						throw new UnsupportedOperationException();
					}

					@Override protected void assertValidContentType(Properties manifestProperties) {
						// we accept all content types!
					}

					@Override protected void readPayload(ZipInputStream zin) throws IOException {
						// we do *not* read the payload - only the MANIFEST
					}
				};
			}
			return acceptContentType(dataFile.getManifestProperties());
		} catch (Exception x) {
			if (logger.isWarnEnabled())
				logger.warn(String.format("acceptContentType: Reading '%s' failed: %s", file.getAbsolutePath(), x), x);
			return false;
		}
	}

	protected boolean acceptContentType(final Properties manifestProperties) {
		assertNotNull(manifestProperties, "manifestProperties");
		final String contentTypeValue = manifestProperties.getProperty(MANIFEST_PROPERTY_CONTENT_TYPE);
		if (getAcceptContentType() == null)
			return true;

		if (! getAcceptContentType().equals(contentTypeValue))
			return false;

		if (getAcceptContentTypeVersionMin() == 0 && getAcceptContentTypeVersionMax() == Integer.MAX_VALUE)
			return true;

		final String contentTypeVersionString = manifestProperties.getProperty(MANIFEST_PROPERTY_CONTENT_TYPE_VERSION);
		if (isEmpty(contentTypeVersionString))
			return false;

		int contentTypeVersion = Integer.parseInt(contentTypeVersionString);
		return getAcceptContentTypeVersionMin() <= contentTypeVersion
				&& getAcceptContentTypeVersionMax() >= contentTypeVersion;
	}
}
