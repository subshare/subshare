package org.subshare.core.io;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class MultiInputStream extends InputStream {

	private final Iterator<? extends InputStreamSource> inputStreamSourcesIterator;

	private InputStream inputStream;

	public MultiInputStream(final InputStreamSource ... inputStreamSources) throws IOException {
		this(Arrays.asList(assertNotNull("inputStreamSources", inputStreamSources)));
	}

	public MultiInputStream(final Collection<? extends InputStreamSource> inputStreamSources) throws IOException {
		this(assertNotNull("inputStreamSources", inputStreamSources).iterator());
	}

	public MultiInputStream(final Iterator<? extends InputStreamSource> inputStreamSourcesIterator) throws IOException {
		this.inputStreamSourcesIterator = assertNotNull("inputStreamSourcesIterator", inputStreamSourcesIterator);
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
		if (inputStream != null) {
			inputStream.close();
			inputStream = null;
		}
	}
}
