package org.subshare.core.io;

import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.io.IInputStream;

/**
 * {@link InputStream} implementation combining multiple other {@code InputStream}s.
 * <p>
 * To avoid multiple {@code InputStream}s from being open at the same time, if possible, this {@code InputStream}
 * takes multiple {@link InputStreamSource}s as input, instead. It then opens only one stream at a time.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class MultiInputStream extends InputStream implements IInputStream {

	private static final Logger logger = LoggerFactory.getLogger(MultiInputStream.class);

	private final Iterator<? extends InputStreamSource> inputStreamSourcesIterator;

	private InputStream inputStream;

	public MultiInputStream(final InputStreamSource ... inputStreamSources) throws IOException {
		this(Arrays.asList(requireNonNull(inputStreamSources, "inputStreamSources")));
	}

	public MultiInputStream(final Collection<? extends InputStreamSource> inputStreamSources) throws IOException {
		this(requireNonNull(inputStreamSources, "inputStreamSources").iterator());
	}

	public MultiInputStream(final Iterator<? extends InputStreamSource> inputStreamSourcesIterator) throws IOException {
		this.inputStreamSourcesIterator = requireNonNull(inputStreamSourcesIterator, "inputStreamSourcesIterator");
		nextInputStream();
	}

	private void nextInputStream() throws IOException {
		if (inputStream != null)
			inputStream.close();

		if (inputStreamSourcesIterator.hasNext())
			inputStream = inputStreamSourcesIterator.next().createInputStream();
		else
			inputStream = null;
	}

	@Override
	public int read() throws IOException {
		int result;
		while ((result = inputStream == null ? -1 : inputStream.read()) < 0) {
			nextInputStream();
			if (inputStream == null)
				break;
		}
		return result;
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		int result;
		while ((result = inputStream == null ? -1 : inputStream.read(b, off, len)) <= 0) {
			nextInputStream();
			if (inputStream == null)
				break;
		}
		return result;
	}

	@Override
	public long skip(final long n) throws IOException {
		long result;
		while ((result = inputStream == null ? 0 : inputStream.skip(n)) <= 0) {
			nextInputStream();
			if (inputStream == null)
				break;
		}
		return result;
	}

	@Override
	public int available() throws IOException {
		int result;
		while ((result = inputStream == null ? 0 : inputStream.available()) <= 0) {
			nextInputStream();
			if (inputStream == null)
				break;
		}
		return result;
	}

	@Override
	public void close() throws IOException {
		final List<Throwable> errors = new ArrayList<>(1);
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (final Exception x) {
				errors.add(x);
				logger.warn("close: Closing underlying InputStream failed: " + x, x);
			}
			inputStream = null;
		}

		while (inputStreamSourcesIterator.hasNext()) {
			final InputStreamSource inputStreamSource = inputStreamSourcesIterator.next();
			try {
				inputStreamSource.discard();
			} catch (final Exception x) {
				errors.add(x);
				logger.warn("close: Discarding underlying InputStreamSource failed: " + x, x);
			}
		}

		if (!errors.isEmpty()) // TODO would be nice to have a MultiException (or named similarly) and pass all causes...
			throw new IOException(errors.get(0));
	}
}
