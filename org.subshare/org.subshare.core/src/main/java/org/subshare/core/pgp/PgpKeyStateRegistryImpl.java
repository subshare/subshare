package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static java.util.Objects.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;

public class PgpKeyStateRegistryImpl extends FileBasedObjectRegistry implements PgpKeyStateRegistry {

	private static final Logger logger = LoggerFactory.getLogger(PgpKeyStateRegistryImpl.class);
	private static final String PAYLOAD_ENTRY_NAME = PgpKeyStateRegistryDto.class.getSimpleName() + ".xml";

	private final File pgpKeyStateRegistryFile;

	private final Map<PgpKeyId, PgpKeyStateDto> pgpKeyId2PgpKeyStateDto = new HashMap<>();
	private Uid version;
	private final Pgp pgp;
	private boolean needUpdateTrustDb;

	private final PropertyChangeListener pgpPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			syncWithPgp();
		}
	};

	private static final class Holder {
		public static final PgpKeyStateRegistryImpl instance = new PgpKeyStateRegistryImpl();
	}

	public static PgpKeyStateRegistry getInstance() {
		return Holder.instance;
	}

	public PgpKeyStateRegistryImpl() {
		pgpKeyStateRegistryFile = createFile(ConfigDir.getInstance().getFile(), PGP_KEY_STATE_REGISTRY_FILE_NAME);
		read();

		pgp = PgpRegistry.getInstance().getPgpOrFail();
		addWeakPropertyChangeListener(pgp, pgpPropertyChangeListener);
		syncWithPgp();
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
//			version = new Uid(); // done by markDirty()
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
			final PgpKeyId pgpKeyId = requireNonNull(pgpKeyStateDto.getPgpKeyId(), "pgpKeyStateDto.pgpKeyId");
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

	protected synchronized void syncWithPgp() {
		logger.info("syncWithPgp: entered.");
		final long startTimestamp = System.currentTimeMillis();

		needUpdateTrustDb = false;

		// All keys that are unmodified in PGP's trust-database, but have meaningful data in the
		// PgpKeyStateRegistry should first be updated in the PGP-trust-database!
		for (final PgpKeyStateDto pgpKeyStateDto : pgpKeyId2PgpKeyStateDto.values()) {
			final PgpKeyId pgpKeyId = requireNonNull(pgpKeyStateDto.getPgpKeyId(), "pgpKeyStateDto.pgpKeyId");
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			if (pgpKey != null && isDefaults(pgpKey)) {
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

		// Then we update *all* keys in the PgpKeyStateRegistry.
		for (final PgpKey pgpKey : pgp.getMasterKeys()) {
			final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
			PgpKeyStateDto pgpKeyStateDto = pgpKeyId2PgpKeyStateDto.get(pgpKeyId);
			if (pgpKeyStateDto == null) {
				if (! isDefaults(pgpKey)) {
					pgpKeyStateDto = createPgpKeyStateDto(pgpKey);
					pgpKeyId2PgpKeyStateDto.put(pgpKeyId, pgpKeyStateDto);
					markDirty();
				}
			}
			else if (isModified(pgpKey, pgpKeyStateDto)) {
				updatePgpKeyStateDto(pgpKey, pgpKeyStateDto);
				markDirty();
			}
		}

		if (needUpdateTrustDb)
			pgp.updateTrustDb();

		writeIfNeeded();
		logger.info("syncWithPgp: leaving (took {} ms).", System.currentTimeMillis() - startTimestamp);
	}

	private PgpKeyStateDto createPgpKeyStateDto(final PgpKey pgpKey) {
		requireNonNull(pgpKey, "pgpKey");
		final PgpKeyStateDto pgpKeyStateDto = new PgpKeyStateDto();
		pgpKeyStateDto.setPgpKeyId(pgpKey.getPgpKeyId());
		pgpKeyStateDto.setCreated(now());
		updatePgpKeyStateDto(pgpKey, pgpKeyStateDto);
		return pgpKeyStateDto;
	}

	private void updatePgpKeyStateDto(final PgpKey pgpKey, final PgpKeyStateDto pgpKeyStateDto) {
		requireNonNull(pgpKey, "pgpKey");
		requireNonNull(pgpKeyStateDto, "pgpKeyStateDto");
		pgpKeyStateDto.setChanged(now());
		pgpKeyStateDto.setDisabled(pgpKey.isDisabled());
		pgpKeyStateDto.setOwnerTrust(pgp.getOwnerTrust(pgpKey));
	}

	private boolean isDefaults(final PgpKey pgpKey) {
		final boolean enabled = ! pgpKey.isDisabled();
		final PgpOwnerTrust ownerTrust = pgp.getOwnerTrust(pgpKey);
		return enabled && ownerTrust == PgpOwnerTrust.UNSPECIFIED;
	}

	private boolean isModified(final PgpKey pgpKey, final PgpKeyStateDto pgpKeyStateDto) {
		requireNonNull(pgpKey, "pgpKey");
		requireNonNull(pgpKeyStateDto, "pgpKeyStateDto");
		return pgpKeyStateDto.isDisabled() != pgpKey.isDisabled()
			|| pgpKeyStateDto.getOwnerTrust() != pgp.getOwnerTrust(pgpKey);
	}

	protected synchronized void mergeFrom(final PgpKeyStateRegistryDto pgpKeyStateRegistryDto) {
		requireNonNull(pgpKeyStateRegistryDto, "pgpKeyStateRegistryDto");
		needUpdateTrustDb = false;

		for (final PgpKeyStateDto inPgpKeyStateDto : pgpKeyStateRegistryDto.getPgpKeyStateDtos()) {
			final PgpKeyId pgpKeyId = requireNonNull(inPgpKeyStateDto.getPgpKeyId(), "inPgpKeyStateDto.pgpKeyId");
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

		writeIfNeeded();
	}

	private void updatePgp(PgpKey pgpKey, PgpKeyStateDto pgpKeyStateDto) {
		requireNonNull(pgpKey, "pgpKey");
		requireNonNull(pgpKeyStateDto, "pgpKeyStateDto");

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
