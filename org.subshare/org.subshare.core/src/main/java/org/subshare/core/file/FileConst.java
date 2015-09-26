package org.subshare.core.file;

public interface FileConst {

	String MANIFEST_PROPERTIES_FILE_NAME = "MANIFEST.properties";

	String MANIFEST_PROPERTIES_SIGNATURE_FILE_NAME = "MANIFEST.properties.sig";

	/**
	 * Content-type specifying what the data in the file is and how it can be read.
	 */
	String MANIFEST_PROPERTY_CONTENT_TYPE = "contentType";

	/**
	 * Version of the content-type. This is the version of the data/file structure - it is not the version of the
	 * data (if there is such version).
	 */
	String MANIFEST_PROPERTY_CONTENT_TYPE_VERSION = "contentTypeVersion";

	String SUBSHARE_FILE_EXTENSION = ".subshare";

}
