package org.subshare.core.fbor;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.core.file.FileConst.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.io.NoCloseInputStream;
import co.codewizards.cloudstore.core.io.NoCloseOutputStream;
import co.codewizards.cloudstore.core.oio.File;

public abstract class FileBasedObjectRegistry {

	private static final long DEFERRED_WRITE_DELAY_MS = 30_000;

	protected FileBasedObjectRegistry() {
		Runtime.getRuntime().addShutdownHook(new Thread(getClass().getSimpleName() + ".shutdownHook") {
			@Override
			public void run() {
				writeIfNeeded();
			}
		});
	}

	private Properties manifestProperties;
	private boolean dirty;

	protected abstract File getFile();
	protected abstract String getContentType();

	private final Timer deferredWriteTimer = new Timer(getClass().getSimpleName() + ".deferredWriteTimer", false); // must *not* be a daemon!
	private TimerTask deferredWriteTimerTask;
	private int ignoreDeferredWriteCounter = 0;

	protected int getContentTypeVersion() {
		return 1;
	}

	protected void read(final InputStream in) throws IOException {
		final ZipInputStream zin = new ZipInputStream(new NoCloseInputStream(assertNotNull("in", in)));

		manifestProperties = readManifest(zin);
		if (manifestProperties == null) { // be fault-tolerant on *empty* input.
			manifestProperties = initManifestProperties();
			return;
		}

		readPayload(zin);

		zin.close();
	}

	protected synchronized void read() {
		enableIgnoreDeferredWrite();
		try {
			try (final LockFile lockFile = acquireLockFile();) {
				try (final InputStream in = lockFile.createInputStream();) {
					read(in);
				}
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
			markClean();
		} finally {
			disableIgnoreDeferredWrite();
		}
	}

	protected synchronized void write() {
		try (LockFile lockFile = acquireLockFile();) {
			try (final OutputStream out = lockFile.createOutputStream();) {
				write(out);
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
		markClean();
	}

	public synchronized void writeIfNeeded() {
		if (isDirty())
			write();
	}

	protected long getDeferredWriteDelayMs() {
		return DEFERRED_WRITE_DELAY_MS;
	}

	protected synchronized void enableIgnoreDeferredWrite() {
		++ignoreDeferredWriteCounter;
	}

	protected synchronized void disableIgnoreDeferredWrite() {
		if (--ignoreDeferredWriteCounter < 0)
			throw new IllegalStateException("ignoreDeferredWriteCounter < 0");
	}

	protected synchronized void deferredWrite() {
		if (ignoreDeferredWriteCounter > 0)
			return;

		if (deferredWriteTimerTask != null) {
			deferredWriteTimerTask.cancel();
			deferredWriteTimerTask = null;
		}

		deferredWriteTimerTask = new TimerTask() {
			@Override
			public void run() {
				synchronized (FileBasedObjectRegistry.this) {
					deferredWriteTimerTask = null;
				}
				write();
			}
		};
		deferredWriteTimer.schedule(deferredWriteTimerTask, getDeferredWriteDelayMs());
	}

	protected Properties readManifest(final ZipInputStream zin) throws IOException {
		assertNotNull("zin", zin);

		final ZipEntry ze = zin.getNextEntry();
		if (ze == null)
			return null; // be fault-tolerant on *empty* input.
//			throw new IllegalArgumentException(String.format("Input data is not valid: It lacks the '%s' as very first zip-entry (there is no first ZipEntry)!", MANIFEST_PROPERTIES_FILE_NAME));

		if (!MANIFEST_PROPERTIES_FILE_NAME.equals(ze.getName()))
			throw new IllegalArgumentException(String.format("Input data is not valid: The very first zip-entry is not '%s' (it is '%s' instead)!", MANIFEST_PROPERTIES_FILE_NAME, ze.getName()));

		final Properties properties = new Properties();
		properties.load(zin);

		final String contentType = getContentType();
		final String ct = properties.getProperty(MANIFEST_PROPERTY_CONTENT_TYPE);
		if (!contentType.equals(ct))
			throw new IllegalArgumentException(String.format("Input data is not valid: The manifest indicates the content-type '%s', but '%s' is expected!", ct, contentType));

		return properties;
	}

	protected Properties initManifestProperties() {
		final Properties manifestProperties = new Properties();
		manifestProperties.setProperty(MANIFEST_PROPERTY_CONTENT_TYPE, getContentType());
		manifestProperties.setProperty(MANIFEST_PROPERTY_CONTENT_TYPE_VERSION, Integer.toString(getContentTypeVersion()));
		return manifestProperties;
	}

	protected void readPayload(final ZipInputStream zin) throws IOException {
		assertNotNull("zin", zin);

		ZipEntry zipEntry;
		while (null != (zipEntry = zin.getNextEntry()))
			readPayloadEntry(zin, zipEntry);
	}

	protected abstract void readPayloadEntry(final ZipInputStream zin, final ZipEntry zipEntry) throws IOException;


	protected LockFile acquireLockFile() {
		return LockFileFactory.getInstance().acquire(getFile(), 30000);
	}

	protected void markClean() {
		dirty = false;
	}

	protected void markDirty() {
		dirty = true;
	}

	protected boolean isDirty() {
		return dirty;
	}


	protected void write(final OutputStream out) throws IOException {
		assertNotNull("out", out);

		final byte[] manifestData = createManifestData();

		final ZipOutputStream zout = new ZipOutputStream(new NoCloseOutputStream(out));

		zout.putNextEntry(createManifestZipEntry(manifestData));
		zout.write(manifestData);
		zout.closeEntry();

		writePayload(zout);
		zout.close();
	}

	/**
	 * Write the actual payload.
	 * <p>
	 * For each payload entry, you must first put a {@link ZipEntry}, then write the binary data and
	 * finally invoke {@link ZipOutputStream#closeEntry() closeEntry()} - for example:
	 * <p>
	 * <pre> for (final Map.Entry&lt;String, byte[]&gt; me : name2ByteArray.entrySet()) {
	 *   final String name = me.getKey();
	 *   zout.putNextEntry(new ZipEntry(name));
	 *   zout.write(me.getValue());
	 *   zout.closeEntry();
	 * }</pre>
	 *
	 * @param zout the {@link ZipOutputStream} to write the actual payload to. Never <code>null</code>.
	 * @throws IOException if writing data failed.
	 */
	protected abstract void writePayload(final ZipOutputStream zout) throws IOException;

	private ZipEntry createManifestZipEntry(final byte[] manifestData) {
		final ZipEntry ze = new ZipEntry(MANIFEST_PROPERTIES_FILE_NAME);
		ze.setMethod(ZipEntry.STORED);
		ze.setSize(manifestData.length);
		ze.setCompressedSize(manifestData.length);
		final CRC32 crc32 = new CRC32();
		crc32.update(manifestData);
		ze.setCrc(crc32.getValue());
		return ze;
	}

	private byte[] createManifestData() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);

		final SortedMap<String,String> sortedManifestProperties = createSortedManifestProperties();

		final String contentType = sortedManifestProperties.remove(MANIFEST_PROPERTY_CONTENT_TYPE);
		assertNotNull(MANIFEST_PROPERTY_CONTENT_TYPE, contentType);
		writeManifestEntry(w, MANIFEST_PROPERTY_CONTENT_TYPE, contentType);

		final String version = sortedManifestProperties.remove(MANIFEST_PROPERTY_CONTENT_TYPE_VERSION);
		assertNotNull(MANIFEST_PROPERTY_CONTENT_TYPE_VERSION, version);
		try {
			Integer.parseInt(version);
		} catch (NumberFormatException x) {
			throw new IllegalStateException(MANIFEST_PROPERTY_CONTENT_TYPE_VERSION + " is not a valid integer: " + version);
		}
		writeManifestEntry(w, MANIFEST_PROPERTY_CONTENT_TYPE_VERSION, version);

		for (Map.Entry<String, String> me : sortedManifestProperties.entrySet())
			writeManifestEntry(w, me.getKey(), me.getValue());

		w.close();
		return out.toByteArray();
	}

	public void mergeFrom(final byte[] data) {
		assertNotNull("data", data);
		if (data.length == 0)
			return;

		try {
			try (final ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(data));) {
				final Properties manifestProperties = readManifest(zin);
				if (!getContentType().equals(manifestProperties.getProperty(MANIFEST_PROPERTY_CONTENT_TYPE)))
					throw new IllegalArgumentException(String.format(
							"data has unexpected contentType: '%s' was found, but '%s' was expected!", getContentType(), manifestProperties.getProperty(MANIFEST_PROPERTY_CONTENT_TYPE)));

				ZipEntry zipEntry;
				while (null != (zipEntry = zin.getNextEntry()))
					mergeFrom(zin, zipEntry);
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	protected abstract void mergeFrom(ZipInputStream zin, ZipEntry zipEntry);

	private SortedMap<String, String> createSortedManifestProperties() {
		final TreeMap<String, String> result = new TreeMap<String, String>();
		for (final Map.Entry<Object, Object> me : manifestProperties.entrySet())
			result.put(me.getKey().toString(), me.getValue().toString());

		return result;
	}

	private void writeManifestEntry(final Writer w, final String key, final String value) throws IOException {
		assertNotNull("w", w);
		assertNotNull("key", key);
		assertNotNull("value", value);

		w.write(key);
		w.write('=');
		w.write(value);
		w.write('\n');
	}
}
