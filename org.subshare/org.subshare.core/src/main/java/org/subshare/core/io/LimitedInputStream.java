package org.subshare.core.io;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static java.util.Objects.*;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.IInputStream;

/**
 * {@code LimitedInputStream} makes sure, not more data is read from the underlying
 * {@code InputStream} as specified by the limit.
 * <p>
 * Thus, you can use an instance of it as a wrapper e.g. for a {@code Socket}'s {@code InputStream}
 * and give it to higher streams like {@link GZIPInputStream} or even a reader + deserializer.
 * Despite the higher streams trying to read ahead and buffer, it is made sure that the underlying
 * stream is not read over the limit.
 *
 * @author Marco Schulze
 * @author Marc Klinger - marc at nightlabs dot de (API documentation fixes)
 */
public class LimitedInputStream extends FilterInputStream implements IInputStream {
	private final int minLimit;
	private final int maxLimit;
	private int readPos = 0;

	/**
	 * Creates a new {@code LimitedInputStream}.
	 * @param in the underlying {@code IInputStream}. Must not be <code>null</code>.
	 * @param minLimit the minimum number of bytes that are expected to be read. If the end of the
	 * stream is reached before, an {@link IOException} is thrown.
	 * @param maxLimit the maximum number of bytes to read from {@code in}.
	 */
	public LimitedInputStream(final IInputStream in, final int minLimit, final int maxLimit)
	{
		this(castStream(in), minLimit, maxLimit);
	}

	/**
	 * Creates a new {@code LimitedInputStream}.
	 * @param in the underlying {@code InputStream}. Must not be <code>null</code>.
	 * @param minLimit the minimum number of bytes that are expected to be read. If the end of the
	 * stream is reached before, an {@link IOException} is thrown.
	 * @param maxLimit the maximum number of bytes to read from {@code in}.
	 */
	public LimitedInputStream(final InputStream in, final int minLimit, final int maxLimit)
	{
		super(requireNonNull(in, "in"));
		this.minLimit = minLimit;
		this.maxLimit = maxLimit;
	}

	public LimitedInputStream(final ByteArrayInputStream in, final int minLimit, final int maxLimit) {
		this((InputStream) in, minLimit, maxLimit);
	}

	@Override
	public int available() throws IOException
	{
		final int reallyAvailable = in.available();

		if (reallyAvailable < 0) {
			if (readPos < minLimit)
				throw new IOException("inputStream is closed; only "+readPos+" Bytes read, but minLimit = "+minLimit+" Bytes!");
		}

		if (reallyAvailable <= 0) // we are never blocking, but if in is blocking and returned 0, we should return 0 as well
			return reallyAvailable;

		int limitedAvailable = Math.min( reallyAvailable, maxLimit - readPos);

		if (limitedAvailable <= 0)
			limitedAvailable = -1;

		return limitedAvailable;
	}

	@Override
	public boolean markSupported()
	{
		return false;
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException("mark/reset not supported");
	}

	@Override
	public int read() throws IOException
	{
		if (readPos < maxLimit) {
			final int res = in.read();

			if (res >= 0)
				readPos++;
			else {
				if (readPos < minLimit)
					throw new IOException("inputStream is closed; only "+readPos+" Bytes read, but minLimit = "+minLimit+" Bytes!");
			}

			return res;
		}
		else
			return -1;
	}

	@Override
	public int read(final byte[] b) throws IOException
	{
		return this.read(b, 0, b.length);
	}

	@Override
	public int read(final byte[] b, final int off, int len) throws IOException
	{
		if (readPos + len > maxLimit) {
			len = maxLimit - readPos;
			if (len == 0)
				return -1;
		}

		if (len > 0) {
			final int bytesRead = in.read(b, off, len);

			if (bytesRead > 0)
				readPos += bytesRead;

			if (bytesRead < 0 && readPos < minLimit)
				throw new IOException("inputStream is closed; only "+readPos+" Bytes read, but minLimit = "+minLimit+" Bytes!");

			return bytesRead;
		}
		else if (len < 0)
			throw new IndexOutOfBoundsException("len < 0!");
		else
			return 0;
	}

	@Override
	public long skip(long len) throws IOException
	{
		if (readPos + len > maxLimit)
			len = maxLimit - readPos;

		if (len > 0) {
			final long bytesSkipped = in.skip(len);

			if (bytesSkipped > 0)
				readPos += bytesSkipped;

			return bytesSkipped;
		}
		else
			return 0;
	}

	/**
	 * Normally closes this {@code InputStream}.
	 * <p>
	 * <b>Important: The implementation in {@link LimitedInputStream} does not do anything!</b> Most importantly, it does
	 * not close the underlying {@code InputStream}!
	 * <p>
	 * The main purpose of this class is to read multiple chunks with a fixed size from an underlying stream. It makes
	 * therefore usually no sense to close it.
	 */
	@Override
	public void close() throws IOException { }
}