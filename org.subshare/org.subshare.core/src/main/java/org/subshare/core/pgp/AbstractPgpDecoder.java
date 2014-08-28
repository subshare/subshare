package org.subshare.core.pgp;

import java.io.InputStream;
import java.io.OutputStream;


public abstract class AbstractPgpDecoder implements PgpDecoder {

	private InputStream inputStream;
	private OutputStream outputStream;
	private PgpKey signPgpKey;

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}
	@Override
	public void setInputStream(final InputStream inputStream) {
		this.inputStream = inputStream;
	}
	protected InputStream getInputStreamOrFail() {
		final InputStream inputStream = getInputStream();
		if (inputStream == null)
			throw new IllegalStateException("inputStream == null");

		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}
	@Override
	public void setOutputStream(final OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	protected OutputStream getOutputStreamOrFail() {
		final OutputStream outputStream = getOutputStream();
		if (outputStream == null)
			throw new IllegalStateException("outputStream == null");

		return outputStream;
	}

	protected PgpAuthenticationCallback getPgpAuthenticationCallback() {
		final PgpAuthenticationCallback pgpAuthenticationCallback = PgpRegistry.getInstance().getPgpAuthenticationCallback();
		return pgpAuthenticationCallback;
	}

	protected PgpAuthenticationCallback getPgpAuthenticationCallbackOrFail() {
		final PgpAuthenticationCallback pgpAuthenticationCallback = getPgpAuthenticationCallback();
		if (pgpAuthenticationCallback == null)
			throw new IllegalStateException("There is no PgpAuthenticationCallback assigned!");

		return pgpAuthenticationCallback;
	}

	@Override
	public PgpKey getSignPgpKey() {
		return signPgpKey;
	}
	protected void setSignPgpKey(final PgpKey signPgpKey) {
		this.signPgpKey = signPgpKey;
	}
}
