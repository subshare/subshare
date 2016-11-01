package org.subshare.core.file;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EncryptedDataFile extends DataFile {
	public static final String CONTENT_TYPE_VALUE = "application/vnd.subshare.encrypted";
	public static final String ENTRY_NAME_DEFAULT_DATA = "default.gpg";
	public static final String ENTRY_NAME_SIGNING_KEY_DATA = "signingKey.gpg";

	public EncryptedDataFile(final byte[] in) throws IOException {
		this(new ByteArrayInputStream(assertNotNull("in", in)));
	}

	public EncryptedDataFile(final InputStream in) throws IOException {
		super(in);
	}

	public EncryptedDataFile() {
		super();
	}

	public void putSigningKeyData(byte[] data) {
		putData(ENTRY_NAME_SIGNING_KEY_DATA, data);
	}
	public byte[] getSigningKeyData() { // optional! might likely not be contained in the file!
		return getData(ENTRY_NAME_SIGNING_KEY_DATA);
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
