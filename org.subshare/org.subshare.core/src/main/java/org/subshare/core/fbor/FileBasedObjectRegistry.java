package org.subshare.core.fbor;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.core.file.FileConst.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.io.NoCloseInputStream;
import co.codewizards.cloudstore.core.io.NoCloseOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;
import co.codewizards.cloudstore.core.util.IOUtil;

public abstract class FileBasedObjectRegistry {

	private static final Logger logger = LoggerFactory.getLogger(FileBasedObjectRegistry.class);

	private static final long DEFERRED_WRITE_DELAY_MS = 30_000;

	/**
	 * Keep this number of backup files.
	 * <p>
	 * There may exist more, though, because no file younger than
	 * {@link #BACKUP_DELETE_MIN_AGE_MSEC} is deleted!
	 * @see #BACKUP_DELETE_MIN_AGE_MSEC
	 */
	private static final int BACKUP_KEEP_MAX_FILE_QTY = 10;

	/**
	 * Delete a backup file only, if its backup timestamp (not the file age) is older
	 * than this number of milliseconds.
	 * @see #BACKUP_KEEP_MAX_FILE_QTY
	 */
	private static final long BACKUP_DELETE_MIN_AGE_MSEC = 2L * 24L * 3600L * 1000L;

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
		final ZipInputStream zin = new ZipInputStream(new NoCloseInputStream(assertNotNull(in, "in")));

		manifestProperties = readManifest(zin);
		if (manifestProperties == null) { // be fault-tolerant on *empty* input.
			manifestProperties = initManifestProperties();
			return;
		}

		readPayload(zin);

		zin.close();
	}

	protected synchronized void read() {
		preRead();
		enableIgnoreDeferredWrite();
		try {
			try (final LockFile lockFile = acquireLockFile();) {
				try (final InputStream in = castStream(lockFile.createInputStream())) {
					read(in);
				}
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
			markClean();
		} finally {
			disableIgnoreDeferredWrite();
		}
		postRead();
	}

	protected void preRead() {
	}

	protected void postRead() {
	}

	protected synchronized void write() {
		try (LockFile lockFile = acquireLockFile();) {
			backup(lockFile);
			try (final OutputStream out = castStream(lockFile.createOutputStream())) {
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
		assertNotNull(zin, "zin");

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
		assertNotNull(zin, "zin");

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
		assertNotNull(out, "out");

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
		assertNotNull(contentType, MANIFEST_PROPERTY_CONTENT_TYPE);
		writeManifestEntry(w, MANIFEST_PROPERTY_CONTENT_TYPE, contentType);

		final String version = sortedManifestProperties.remove(MANIFEST_PROPERTY_CONTENT_TYPE_VERSION);
		assertNotNull(version, MANIFEST_PROPERTY_CONTENT_TYPE_VERSION);
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
		assertNotNull(data, "data");
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
		assertNotNull(w, "w");
		assertNotNull(key, "key");
		assertNotNull(value, "value");

		w.write(key);
		w.write('=');
		w.write(value);
		w.write('\n');
	}

	private void backup(final LockFile lockFile) throws IOException {
		final File origFile = assertNotNull(lockFile, "lockFile").getFile();
		if (origFile.length() == 0)
			return;

		final File backupDir = origFile.getParentFile().createFile("backup");
		if (! backupDir.isDirectory())
			backupDir.mkdir();

		if (! backupDir.isDirectory())
			throw new IOException("Creating directory failed: " + backupDir.getAbsolutePath());

		final String origFileName = origFile.getName();
		final String fileNameWithoutExtension = getFileNameWithoutExtension(origFileName);
		final String fileExtension = getFileExtension(origFileName);

		deleteOldBackupFiles(backupDir, fileNameWithoutExtension, fileExtension);

		final File backupFile = backupDir.createFile(
				String.format("%s.%s.%s", fileNameWithoutExtension, Long.toString(System.currentTimeMillis(), 36), fileExtension));

		try (final InputStream in = castStream(lockFile.createInputStream())) {
			try (final OutputStream out = castStream(backupFile.createOutputStream())) {
				transferStreamData(in, out);
			}
		}
		backupFile.setLastModified(origFile.lastModified());
	}

	private void deleteOldBackupFiles(final File backupDir, final String fileNameWithoutExtension, final String fileExtension) {
		assertNotNull(backupDir, "backupDir");
		assertNotNull(fileNameWithoutExtension, "fileNameWithoutExtension");

		List<File> backupFiles = getBackupFiles(backupDir, fileNameWithoutExtension, fileExtension);
		SortedMap<Long, File> backupTimestamp2File = new TreeMap<>();

		for (File backupFile : backupFiles) {
			try {
				final String name = backupFile.getName();
				final String fnwe = getFileNameWithoutExtension(name);
				final String backupTimestampString = assertNotNull(getFileExtension(fnwe), "getFileExtension('" + fnwe + "')");
				final long backupTimestamp = Long.parseLong(backupTimestampString, 36);
				backupTimestamp2File.put(backupTimestamp, backupFile);
			} catch (Exception x) {
				logger.error("deleteOldBackupFiles: " + x, x);
				backupFile.delete();
			}
		}

		final int maxFilesToDelete = Math.max(0,
				backupTimestamp2File.size() - BACKUP_KEEP_MAX_FILE_QTY + 1); // +1, because we're just about to create yet one more backup-file.
		final long maxBackupTimestampToDelete = System.currentTimeMillis() - BACKUP_DELETE_MIN_AGE_MSEC;

		int deletedFilesQty = 0;
		for (final Map.Entry<Long, File> me : backupTimestamp2File.entrySet()) {
			if (deletedFilesQty >= maxFilesToDelete)
				return;

			final long backupTimestamp = me.getKey();
			if (backupTimestamp >= maxBackupTimestampToDelete)
				return;

			final File backupFile = me.getValue();

			backupFile.delete();
			if (backupFile.exists())
				logger.error("Deleting file failed: {}", backupFile.getAbsolutePath());

			++deletedFilesQty;
		}
	}

	/**
	 * Gets the backup-files in the given {@code backupDir} matching the given name-without-extension and
	 * extension.
	 * @param backupDir the backup-directory. Must not be <code>null</code>.
	 * @param fileNameWithoutExtension the file-name without file-extension. Must not be <code>null</code>.
	 * See {@link IOUtil#getFileNameWithoutExtension(String)}.
	 * @param fileExtension the file-extension or <code>null</code>. See {@link IOUtil#getFileExtension(String)}.
	 * @return the matching backup-files. Never <code>null</code>.
	 */
	private List<File> getBackupFiles(final File backupDir, final String fileNameWithoutExtension, final String fileExtension) {
		assertNotNull(backupDir, "backupDir");
		assertNotNull(fileNameWithoutExtension, "fileNameWithoutExtension");
		final String fileNameWithoutExtensionWithDot = fileNameWithoutExtension + '.';

		File[] files = backupDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				final String name = file.getName();
				final String fnwe = getFileNameWithoutExtension(name);
				final String fe = getFileExtension(name);
				return fnwe.startsWith(fileNameWithoutExtensionWithDot) && equal(fileExtension, fe);
			}
		});

		return Arrays.asList(files);
	}
}
