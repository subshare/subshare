package org.subshare.core.file;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EncryptedDataFile extends DataFile {
	public static final String CONTENT_TYPE_VALUE = "application/vnd.subshare.encrypted";
	public static final String ENTRY_NAME_DEFAULT_DATA = "default.gpg";

	public EncryptedDataFile(final byte[] in) throws IOException {
		this(new ByteArrayInputStream(assertNotNull("in", in)));
	}

	public EncryptedDataFile(final InputStream in) throws IOException {
		super(in);
	}

	public EncryptedDataFile() {
		super();
	}

	public void putDefaultData(byte[] data) {
		putData(ENTRY_NAME_DEFAULT_DATA, data);
	}

	public byte[] getDefaultData() {
		return getData(ENTRY_NAME_DEFAULT_DATA);
	}

	@Override
	protected String getContentTypeValue() {
		return CONTENT_TYPE_VALUE;
	}
}
