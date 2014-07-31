package org.subshare.core.crypto;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.io.LimitedInputStream;

import co.codewizards.cloudstore.core.util.IOUtil;

public class AsymCombiDecrypterInputStream extends FilterInputStream {

	private final AsymmetricKeyParameter privateKey;
	private final Header header;
	private final InputStream symIn;

	public AsymCombiDecrypterInputStream(final InputStream in, final AsymmetricKeyParameter privateKey) throws IOException {
		super(assertNotNull("in", in));
		this.privateKey = assertNotNull("privateKey", privateKey);
		this.header = readHeader();
		symIn = new DecrypterInputStream(in, header.symmetricKey);
	}

	private Header readHeader() throws IOException {
		final int version = in.read();
		if (version != 1)
			throw new IOException("version != 1 :: version == " + version);

		final int encryptedSymmetricKeyLength = readOrFail() + (readOrFail() << 8);

		final KeyParameter symmetricKey;
		try (
				final DecrypterInputStream asymIn = new DecrypterInputStream(
						new LimitedInputStream(in, encryptedSymmetricKeyLength, encryptedSymmetricKeyLength),
						privateKey);
		) { // The LimitedInputStream does *not* delegate close()! Therefore, this try-with-resource is safe.
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			IOUtil.transferStreamData(asymIn, bout);
			symmetricKey = new KeyParameter(bout.toByteArray());
		}
		return new Header(version, symmetricKey);
	}

	protected static class Header {
		public final int version;
		public final KeyParameter symmetricKey;
		public Header(final int version, final KeyParameter symmetricKey) {
			this.version = version;
			this.symmetricKey = assertNotNull("symmetricKey", symmetricKey);
		}
	}

	@Override
	public int read() throws IOException {
		return symIn.read();
	}

	@Override
	public int read(final byte[] b) throws IOException {
		return symIn.read(b);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		return symIn.read(b, off, len);
	}

	@Override
	public long skip(final long n) throws IOException {
		return symIn.skip(n);
	}

	@Override
	public int available() throws IOException {
		return symIn.available();
	}

	@Override
	public void close() throws IOException {
		symIn.close();
	}

	@Override
	public void mark(final int readlimit) {
		symIn.mark(readlimit);
	}

	@Override
	public void reset() throws IOException {
		symIn.reset();
	}

	@Override
	public boolean markSupported() {
		return symIn.markSupported();
	}

	private int readOrFail() throws IOException {
		return IOUtil.readOrFail(in);
	}
}
