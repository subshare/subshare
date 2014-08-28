package org.subshare.core.pgp;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractPgp implements Pgp {

	@Override
	public PgpEncoder createEncoder(final InputStream in, final OutputStream out) {
		final PgpEncoder encoder = _createEncoder();
		encoder.setInputStream(in);
		encoder.setOutputStream(out);
		return encoder;
	}

	protected abstract PgpEncoder _createEncoder();

	@Override
	public PgpDecoder createDecoder(final InputStream in, final OutputStream out) {
		final PgpDecoder decoder = _createDecoder();
		decoder.setInputStream(in);
		decoder.setOutputStream(out);
		return decoder;
	}

	protected abstract PgpDecoder _createDecoder();

}
