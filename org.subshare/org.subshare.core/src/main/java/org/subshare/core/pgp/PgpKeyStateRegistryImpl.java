package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.PgpKeyStateDto;
import org.subshare.core.dto.PgpKeyStateRegistryDto;
import org.subshare.core.dto.jaxb.PgpKeyStateRegistryDtoIo;
import org.subshare.core.fbor.FileBasedObjectRegistry;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class PgpKeyStateRegistryImpl extends FileBasedObjectRegistry implements PgpKeyStateRegistry {

	private static final Logger logger = LoggerFactory.getLogger(PgpKeyStateRegistryImpl.class);
	private static final String PAYLOAD_ENTRY_NAME = PgpKeyStateRegistryDto.class.getSimpleName() + ".xml";

	private final File pgpKeyStateRegistryFile;

	private final Map<PgpKeyId, PgpKeyStateDto> pgpKeyId2PgpKeyStateDto = new HashMap<>();
	private Uid version;
	private Pgp pgp;
	private boolean needUpdateTrustDb;

	private static final class Holder {
		public static final PgpKeyStateRegistryImpl instance = new PgpKeyStateRegistryImpl();
	}

	public static PgpKeyStateRegistry getInstance() {
		return Holder.instance;
	}

	public PgpKeyStateRegistryImpl() {
		pgpKeyStateRegistryFile = createFile(ConfigDir.getInstance().getFile(), PGP_KEY_STATE_REGISTRY_FILE_NAME);
		read();
	}

	@Override
	protected File getFile() {
		return pgpKeyStateRegistryFile;
	}

	@Override
	protected String getContentType() {
		return "application/vnd.subshare.pgp-key-state-registry";
	}

	@Override
	protected void preRead() {
		pgpKeyId2PgpKeyStateDto.clear();
		version = null;
	}

	@Override
	protected void postRead() {
		if (version == null) {
			version = new Uid();
			markDirty();
		}
	}

	@Override
	protected void markDirty() {
		super.markDirty();
		version = new Uid();
	}

	@Override
	protected void readPayloadEntry(ZipInputStream zin, ZipEntry zipEntry) throws IOException {
		if (!PAYLOAD_ENTRY_NAME.equals(zipEntry.getName())) {
			logger.warn("readPayloadEntry: Ignoring unexpected zip-entry: {}", zipEntry.getName());
			return;
		}
		final PgpKeyStateRegistryDtoIo pgpKeyStateRegistryDtoIo = new PgpKeyStateRegistryDtoIo();
		final PgpKeyStateRegistryDto pgpKeyStateRegistryDto = pgpKeyStateRegistryDtoIo.deserialize(zin);

		for (PgpKeyStateDto pgpKeyStateDto : pgpKeyStateRegistryDto.getPgpKeyStateDtos()) {
			final PgpKeyId pgpKeyId = assertNotNull("pgpKeyStateDto.pgpKeyId", pgpKeyStateDto.getPgpKeyId());
			pgpKeyId2PgpKeyStateDto.put(pgpKeyId, pgpKeyStateDto);
		}

		version = pgpKeyStateRegistryDto.getVersion();
	}

	@Override
	protected void writePayload(ZipOutputStream zout) throws IOException {
		final PgpKeyStateRegistryDtoIo pgpKeyStateRegistryDtoIo = new PgpKeyStateRegistryDtoIo();
		final PgpKeyStateRegistryDto pgpKeyStateRegistryDto = createPgpKeyStateRegistryDto();

		zout.putNextEntry(new ZipEntry(PAYLOAD_ENTRY_NAME));
		pgpKeyStateRegistryDtoIo.serialize(pgpKeyStateRegistryDto, zout);
		zout.closeEntry();
	}

	private PgpKeyStateRegistryDto createPgpKeyStateRegistryDto() {
		final PgpKeyStateRegistryDto pgpKeyStateRegistryDto = new PgpKeyStateRegistryDto();
		for (final PgpKeyStateDto pgpKeyStateDto : pgpKeyId2PgpKeyStateDto.values())
			pgpKeyStateRegistryDto.getPgpKeyStateDtos().add(pgpKeyStateDto);

		pgpKeyStateRegistryDto.setVersion(version);
		return pgpKeyStateRegistryDto;
	}

	@Override
	protected void mergeFrom(ZipInputStream zin, ZipEntry zipEntry) {
		if (PAYLOAD_ENTRY_NAME.equals(zipEntry.getName())) {
			final PgpKeyStateRegistryDtoIo pgpKeyStateRegistryDtoIo = new PgpKeyStateRegistryDtoIo();
			final PgpKeyStateRegistryDto pgpKeyStateRegistryDto = pgpKeyStateRegistryDtoIo.deserialize(zin);
			mergeFrom(pgpKeyStateRegistryDto);
		}
	}

	public Pgp getPgp() {
		if (pgp == null)
			pgp = PgpRegistry.getInstance().getPgpOrFail();

		return pgp;
	}

	public synchronized void syncWithPgp() {
		final Pgp pgp = getPgp();
		needUpdateTrustDb = false;

		// All keys that are unmodified in PGP's trust-database, but have meaningful data in the
		// PgpKeyStateRegistry should first be updated in the PGP-trust-database!
		for (final PgpKeyStateDto pgpKeyStateDto : pgpKeyId2PgpKeyStateDto.values()) {
//			if (pgpKeyStateDto.getDeleted() != null)
//				continue;

			final PgpKeyId pgpKeyId = assertNotNull("pgpKeyStateDto.pgpKeyId", pgpKeyStateDto.getPgpKeyId());
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			if (pgpKey != null) {

				if (! pgpKey.isDisabled() && pgp.getOwnerTrust(pgpKey) == PgpOwnerTrust.UNSPECIFIED) {
					if (pgpKeyStateDto.isDisabled()) {
						needUpdateTrustDb = true;
						pgp.setDisabled(pgpKey, true);
					}

					if (pgpKeyStateDto.getOwnerTrust() != PgpOwnerTrust.UNSPECIFIED) {
						needUpdateTrustDb = true;
						pgp.setOwnerTrust(pgpKey, pgpKeyStateDto.getOwnerTrust());
					}
				}
			}
		}

		// Then we update *all* keys in the PgpKeyStateRegistry.
		for (final PgpKey pgpKey : pgp.getMasterKeys()) {
			final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
			PgpKeyStateDto pgpKeyStateDto = pgpKeyId2PgpKeyStateDto.get(pgpKeyId);
			if (pgpKeyStateDto == null) {
				pgpKeyStateDto = createPgpKeyStateDto(pgpKey);
				pgpKeyId2PgpKeyStateDto.put(pgpKeyId, pgpKeyStateDto);
				markDirty();
			}
//			else if (pgpKeyStateDto.getDeleted() != null || isModified(pgpKey, pgpKeyStateDto)) {
			else if (isModified(pgpKey, pgpKeyStateDto)) {
				updatePgpKeyStateDto(pgpKey, pgpKeyStateDto);
				markDirty();
			}
		}

//		for (final PgpKeyStateDto pgpKeyStateDto : pgpKeyId2PgpKeyStateDto.values()) {
//			if (pgpKeyStateDto.getDeleted() != null)
//				continue;
//
//			final PgpKeyId pgpKeyId = assertNotNull("pgpKeyStateDto.pgpKeyId", pgpKeyStateDto.getPgpKeyId());
//			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
//			if (pgpKey == null) {
//				if (pgpKeyStateDto.getReadded() != null)
//					pgpKeyStateDto.setReadded(null);
//
//				pgpKeyStateDto.setDeleted(new Date());
//				markDirty();
//			}
//		}

		if (needUpdateTrustDb)
			pgp.updateTrustDb();

		this.pgp = null; // no need to keep reference.
		writeIfNeeded();
	}

	private PgpKeyStateDto createPgpKeyStateDto(final PgpKey pgpKey) {
		assertNotNull("pgpKey", pgpKey);
		final PgpKeyStateDto pgpKeyStateDto = new PgpKeyStateDto();
		pgpKeyStateDto.setPgpKeyId(pgpKey.getPgpKeyId());
		pgpKeyStateDto.setCreated(new Date());
		updatePgpKeyStateDto(pgpKey, pgpKeyStateDto);
		return pgpKeyStateDto;
	}

	private void updatePgpKeyStateDto(final PgpKey pgpKey, final PgpKeyStateDto pgpKeyStateDto) {
		assertNotNull("pgpKey", pgpKey);
		assertNotNull("pgpKeyStateDto", pgpKeyStateDto);
		pgpKeyStateDto.setChanged(new Date());
		pgpKeyStateDto.setDisabled(pgpKey.isDisabled());
		pgpKeyStateDto.setOwnerTrust(getPgp().getOwnerTrust(pgpKey));

//		if (pgpKeyStateDto.getDeleted() != null) {
//			pgpKeyStateDto.setReadded(new Date());
//			pgpKeyStateDto.setDeleted(null);
//		}
	}

	private boolean isModified(final PgpKey pgpKey, final PgpKeyStateDto pgpKeyStateDto) {
		assertNotNull("pgpKey", pgpKey);
		assertNotNull("pgpKeyStateDto", pgpKeyStateDto);
		return pgpKeyStateDto.isDisabled() != pgpKey.isDisabled()
			|| pgpKeyStateDto.getOwnerTrust() != getPgp().getOwnerTrust(pgpKey);
	}

	protected synchronized void mergeFrom(final PgpKeyStateRegistryDto pgpKeyStateRegistryDto) {
		assertNotNull("pgpKeyStateRegistryDto", pgpKeyStateRegistryDto);
		final Pgp pgp = getPgp();
		needUpdateTrustDb = false;

		for (final PgpKeyStateDto inPgpKeyStateDto : pgpKeyStateRegistryDto.getPgpKeyStateDtos()) {
			final PgpKeyId pgpKeyId = assertNotNull("inPgpKeyStateDto.pgpKeyId", inPgpKeyStateDto.getPgpKeyId());
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);

			PgpKeyStateDto myPgpKeyStateDto = pgpKeyId2PgpKeyStateDto.get(pgpKeyId);
			if (myPgpKeyStateDto == null
					|| myPgpKeyStateDto.getChanged().compareTo(inPgpKeyStateDto.getChanged()) < 0) {

				myPgpKeyStateDto = inPgpKeyStateDto;
				pgpKeyId2PgpKeyStateDto.put(pgpKeyId, myPgpKeyStateDto);

				if (pgpKey != null)
					updatePgp(pgpKey, myPgpKeyStateDto);

				markDirty();
			}
		}

		if (needUpdateTrustDb)
			pgp.updateTrustDb();

		this.pgp = null; // no need to keep reference.
		writeIfNeeded();
	}

	private void updatePgp(PgpKey pgpKey, PgpKeyStateDto pgpKeyStateDto) {
		assertNotNull("pgpKey", pgpKey);
		assertNotNull("pgpKeyStateDto", pgpKeyStateDto);
		final Pgp pgp = getPgp();

		if (pgpKey.isDisabled() != pgpKeyStateDto.isDisabled()) {
			needUpdateTrustDb = true;
			pgp.setDisabled(pgpKey, pgpKeyStateDto.isDisabled());
		}

		if (pgpKeyStateDto.getOwnerTrust() != PgpOwnerTrust.UNSPECIFIED) {
			needUpdateTrustDb = true;
			pgp.setOwnerTrust(pgpKey, pgpKeyStateDto.getOwnerTrust());
		}
	}

	public Uid getVersion() {
		return version;
	}
}
