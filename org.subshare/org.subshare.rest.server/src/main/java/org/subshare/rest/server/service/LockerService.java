package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.bouncycastle.util.io.Streams;
import org.subshare.core.locker.LockerEncryptedDataFile;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.PgpSignature;
import org.subshare.rest.server.LockerDir;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.dto.UidList;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

@Path("_Locker")
public class LockerService {

	private static final String DATA_FILE_SUFFIX = ".data";

	@PathParam("pgpKeyId")
	private PgpKeyId pgpKeyId;

	@PathParam("lockerContentName")
	private String lockerContentName;

	private final File lockerDir;
	private final Pgp pgp;

	public LockerService() {
		lockerDir = LockerDir.getInstance().getFile();
		pgp = PgpRegistry.getInstance().getPgpOrFail();
	}

	@GET
	@Path("{pgpKeyId}/{lockerContentName}")
	@Produces(MediaType.APPLICATION_XML)
	public UidList getLockerContentVersions() {
		assertNotNull("pgpKeyId", pgpKeyId);
		assertNotNull("lockerContentName", lockerContentName);

		final UidList result = new UidList();
		final File dir = createFile(lockerDir, pgpKeyId.toString(), lockerContentName);
		final File[] children = dir.listFiles();
		if (children != null) {
			for (final File file : children) {
				final String fileName = file.getName();
				if (!fileName.endsWith(DATA_FILE_SUFFIX))
					continue;

				final String s = fileName.substring(0, fileName.length() - DATA_FILE_SUFFIX.length());
				result.add(new Uid(s));
			}
		}
		return result;
	}

	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public void putLockerEncryptedDataFile(final InputStream inputStream) throws IOException {
		final byte[] input = safeRead(inputStream);

		final LockerEncryptedDataFile encryptedDataFile = new LockerEncryptedDataFile(input);
		PgpSignature pgpSignature = encryptedDataFile.assertManifestSignatureValid();

		final PgpKeyId signaturePgpKeyId = pgpSignature.getPgpKeyId(); // likely a sub-key
		assertNotNull("pgpSignature.pgpKeyId", signaturePgpKeyId);
		final PgpKey signaturePgpKey = pgp.getPgpKey(signaturePgpKeyId);
		assertNotNull("pgp.getPgpKey(signaturePgpKeyId=" + signaturePgpKeyId + ")", signaturePgpKey);

		pgpKeyId = signaturePgpKey.getMasterKey().getPgpKeyId();

		lockerContentName = encryptedDataFile.getContentName();
		assertNotNull("encryptedDataFile.contentName", lockerContentName);

		final Uid lockerContentVersion = encryptedDataFile.getContentVersion();
		assertNotNull("encryptedDataFile.contentVersion", lockerContentVersion);

// We cannot verify the signatures of the signed+encrypted data, because OpenPGP first signs and then encrypts.
// This verification is thus only possible on the client-side (it's done in
//		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
//		for (final String name : encryptedDataFile.getDataNames()) {
//			if (MANIFEST_PROPERTIES_SIGNATURE_FILE_NAME.equals(name))
//				continue;
//
//			final PgpDecoder decoder = pgp.createDecoder(new ByteArrayInputStream(encryptedDataFile.getData(name)), new NullOutputStream());
//			decoder.decode();
//			pgpSignature = decoder.getPgpSignature();
//			if (pgpSignature == null)
//				throw new SignatureException(String.format("Missing signature! name='%s'", name));
//
//			if (!pgpKeyId.equals(pgpSignature.getPgpKeyId()))
//				throw new SignatureException(String.format("Manifest signature's pgpKeyId does not match data's (name='%s') signature's pgpKeyId! %s != %s",
//						name, pgpKeyId, pgpSignature.getPgpKeyId()));
//		}

		final File file = createFile(lockerDir, pgpKeyId.toString(), lockerContentName, lockerContentVersion.toString() + DATA_FILE_SUFFIX);
		file.getParentFile().mkdirs();
		try (final LockFile lockFile = LockFileFactory.getInstance().acquire(file, 30000);) {
			if (lockFile.getFile().length() == input.length) {
				final ByteArrayInputStream newIn = new ByteArrayInputStream(input);
				try (final InputStream oldIn = lockFile.createInputStream();) {
					if (compareInputStreams(newIn, oldIn))
						return; // no need to write - same data already written before
				}
			}
			try (final OutputStream out = lockFile.createOutputStream();) {
				out.write(input);
			}
		}

		for (Uid replacedContentVersion : encryptedDataFile.getReplacedContentVersions()) {
			final File f = createFile(file.getParentFile(), replacedContentVersion.toString() + DATA_FILE_SUFFIX);
			f.delete();
			if (f.exists())
				throw new IOException("Deleting file failed: " + f);
		}
	}

	@GET
	@Path("{pgpKeyId}/{lockerContentName}/{version}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getLockerEncryptedDataFile(@PathParam("version") final String version) {
		assertNotNull("version", version);
		assertNotNull("pgpKeyId", pgpKeyId);
		assertNotNull("lockerContentName", lockerContentName);

		final File file = createFile(lockerDir, pgpKeyId.toString(), lockerContentName, version.toString() + DATA_FILE_SUFFIX);
		if (! file.exists())
			return Response.status(Status.NOT_FOUND).build();

		final StreamingOutput result = new StreamingOutput() {
			@Override
			public void write(final OutputStream out) throws IOException, WebApplicationException {
				final File deletedFileToDeleteAgain;
				try (final LockFile lockFile = LockFileFactory.getInstance().acquire(file, 30000);) {
					try (final InputStream in = lockFile.createInputStream();) {
						Streams.pipeAll(in, out);
						out.flush();
					}
					final File f = lockFile.getFile();
					deletedFileToDeleteAgain = f.length() == 0 ? f : null;
				}

				// In case the file was deleted between the check at the beginning of this method and this StreamingOutput's activity,
				// we delete it again. We assume that it was deleted, if it is empty, because we now that all files we put into the
				// locker are never empty - actually they are LockerEncryptedDataFile instances that must have header + signature.
				if (deletedFileToDeleteAgain != null)
					deletedFileToDeleteAgain.delete();
			}
		};
		return Response.ok(result).build();
	}

	private static byte[] safeRead(final InputStream inputStream) throws IOException {
		assertNotNull("inputStream", inputStream);

		// To protect this server, we throw an exception, if the client tries to upload more than this:
		final int maxBytesLimit = 5 /* MiB */ * 1024 /* KiB */ * 1024 /* B */;

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buf = new byte[64 * 1024];
		int totalBytesRead = 0;
		int bytesRead;
		while ((bytesRead = inputStream.read(buf)) >= 0) {
			if (bytesRead > 0) {
				totalBytesRead += bytesRead;
				if (totalBytesRead > maxBytesLimit)
					throw new IOException(String.format("Input buffer size (%d B) exceeded!", maxBytesLimit));

				out.write(buf, 0, bytesRead);
			}
		}
		return out.toByteArray();
	}
}
