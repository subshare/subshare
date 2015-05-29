package org.subshare.core.file;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.util.io.Streams;
import org.subshare.core.io.NullOutputStream;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpEncoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.PgpSignature;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.io.NoCloseInputStream;
import co.codewizards.cloudstore.core.io.NoCloseOutputStream;

public class EncryptedDataFile {
	public static final String CONTENT_TYPE_VALUE = "application/vnd.subshare.encrypted";
	public static final String ENTRY_NAME_DEFAULT_DATA = "default.gpg";

	private Properties manifestProperties;
	private final Map<String, byte[]> name2ByteArray = new HashMap<>();

	public EncryptedDataFile(final byte[] in) throws IOException {
		this(new ByteArrayInputStream(assertNotNull("in", in)));
	}

	public EncryptedDataFile(final InputStream in) throws IOException {
		assertNotNull("in", in);
		read(in);
	}

	public EncryptedDataFile() {
		initManifestProperties();
	}

	public Properties getManifestProperties() {
		return manifestProperties;
	}

	public void putDefaultData(byte[] data) {
		putData(ENTRY_NAME_DEFAULT_DATA, data);
	}

	public void putData(final String name, final byte[] data) {
		assertNotNull("name", name);
		assertNotNull("data", data);
		name2ByteArray.put(name, data);
	}

	public Set<String> getDataNames() {
		return Collections.unmodifiableSet(name2ByteArray.keySet());
	}

	public byte[] getData(final String name) {
		assertNotNull("name", name);
		return name2ByteArray.get(name);
	}

	public byte[] getDefaultData() {
		return getData(ENTRY_NAME_DEFAULT_DATA);
	}

	private void read(final InputStream in) throws IOException {
		final ZipInputStream zin = new ZipInputStream(new NoCloseInputStream(assertNotNull("in", in)));

		manifestProperties = readManifest(zin);
		if (manifestProperties == null) { // be fault-tolerant on *empty* input.
			initManifestProperties();
			return;
		}

		ZipEntry zipEntry;
		while (null != (zipEntry = zin.getNextEntry())) {
			final String name = zipEntry.getName();
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			Streams.pipeAll(zin, out);
			name2ByteArray.put(name, out.toByteArray());
		}

		zin.close();
	}

	private void initManifestProperties() {
		manifestProperties = new Properties();
		manifestProperties.setProperty(MANIFEST_PROPERTY_CONTENT_TYPE, CONTENT_TYPE_VALUE);
		manifestProperties.setProperty(MANIFEST_PROPERTY_VERSION, Integer.toString(1));
	}

	private Properties readManifest(final ZipInputStream zin) throws IOException {
		assertNotNull("zin", zin);

		final ZipEntry ze = zin.getNextEntry();
		if (ze == null)
			return null; // be fault-tolerant on *empty* input.
//			throw new IllegalArgumentException(String.format("Input data is not valid: It lacks the '%s' as very first zip-entry (there is no first ZipEntry)!", MANIFEST_PROPERTIES_FILE_NAME));

		if (!MANIFEST_PROPERTIES_FILE_NAME.equals(ze.getName()))
			throw new IllegalArgumentException(String.format("Input data is not valid: The very first zip-entry is not '%s' (it is '%s' instead)!", MANIFEST_PROPERTIES_FILE_NAME, ze.getName()));

		final Properties properties = new Properties();
		properties.load(zin);

		final String contentType = properties.getProperty(MANIFEST_PROPERTY_CONTENT_TYPE);
		if (!CONTENT_TYPE_VALUE.equals(contentType))
			throw new IllegalArgumentException(String.format("Input data is not valid: The manifest indicates the content-type '%s', but '%s' is expected!", contentType, CONTENT_TYPE_VALUE));

		return properties;
	}

	public byte[] write() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		write(out);
		return out.toByteArray();
	}

	public void write(final OutputStream out) throws IOException {
		assertNotNull("out", out);

		final byte[] manifestData = createManifestData();

		final byte[] detachedSignatureData = name2ByteArray.get(MANIFEST_PROPERTIES_SIGNATURE_FILE_NAME);
		if (detachedSignatureData != null) {
			if (!isSignatureValid(manifestData, detachedSignatureData))
				name2ByteArray.remove(MANIFEST_PROPERTIES_SIGNATURE_FILE_NAME);
		}

		final ZipOutputStream zout = new ZipOutputStream(new NoCloseOutputStream(out));

		zout.putNextEntry(createManifestZipEntry(manifestData));
		zout.write(manifestData);
		zout.closeEntry();

		for (final Map.Entry<String, byte[]> me : name2ByteArray.entrySet()) {
			final String name = me.getKey();
			zout.putNextEntry(new ZipEntry(name));
			zout.write(me.getValue());
			zout.closeEntry();
		}
		zout.close();
	}

	public void signManifestData(final PgpKey signPgpKey) throws IOException {
		final byte[] manifestData = createManifestData();
		signManifestData(signPgpKey, manifestData);
	}

	protected void signManifestData(final PgpKey signPgpKey, final byte[] manifestData) throws IOException {
		assertNotNull("signPgpKey", signPgpKey);
		assertNotNull("manifestData", manifestData);

		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		final PgpEncoder encoder = pgp.createEncoder(new ByteArrayInputStream(manifestData), new NullOutputStream());
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		encoder.setSignOutputStream(out);
		encoder.setSignPgpKey(signPgpKey);
		encoder.encode();

		final byte[] detachedSignatureData = out.toByteArray();
		name2ByteArray.put(MANIFEST_PROPERTIES_SIGNATURE_FILE_NAME, detachedSignatureData);
	}

	private boolean isSignatureValid(final byte[] signedData, final byte[] detachedSignatureData) {
		assertNotNull("signedData", signedData);
		assertNotNull("detachedSignatureData", detachedSignatureData);
		try {
			assertSignatureValid(signedData, detachedSignatureData);
			return true;
		} catch (SignatureException | IOException x) {
			return false;
		}
	}

	private PgpSignature assertSignatureValid(final byte[] signedData, final byte[] detachedSignatureData) throws SignatureException, IOException {
		assertNotNull("signedData", signedData);
		assertNotNull("detachedSignatureData", detachedSignatureData);
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		final PgpDecoder decoder = pgp.createDecoder(new ByteArrayInputStream(signedData), new NullOutputStream());
		decoder.setSignInputStream(new ByteArrayInputStream(detachedSignatureData));
		decoder.decode();
		return decoder.getPgpSignature();
	}

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

		final String version = sortedManifestProperties.remove(MANIFEST_PROPERTY_VERSION);
		assertNotNull(MANIFEST_PROPERTY_VERSION, version);
		try {
			Integer.parseInt(version);
		} catch (NumberFormatException x) {
			throw new IllegalStateException(MANIFEST_PROPERTY_VERSION + " is not a valid integer: " + version);
		}
		writeManifestEntry(w, MANIFEST_PROPERTY_VERSION, version);

		for (Map.Entry<String, String> me : sortedManifestProperties.entrySet())
			writeManifestEntry(w, me.getKey(), me.getValue());

		w.close();
		return out.toByteArray();
	}

	public boolean isManifestSigned() {
		return name2ByteArray.get(MANIFEST_PROPERTIES_SIGNATURE_FILE_NAME) != null;
	}

	public PgpSignature assertManifestSignatureValid() throws SignatureException {
		final byte[] manifestSignatureData = name2ByteArray.get(MANIFEST_PROPERTIES_SIGNATURE_FILE_NAME);
		if (manifestSignatureData == null)
			throw new SignatureException(String.format("There is no signature! No entry named '%s' found!", MANIFEST_PROPERTIES_SIGNATURE_FILE_NAME));

		final byte[] manifestData;
		try {
			manifestData = createManifestData();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			return assertSignatureValid(manifestData, manifestSignatureData);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

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
