package org.subshare.local.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

import co.codewizards.cloudstore.local.persistence.FileChunk;

@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy = DiscriminatorStrategy.VALUE_MAP, value = "SsFileChunk")
public class SsFileChunk extends FileChunk {

	@Column(defaultValue = "0")
	private int paddingLength;

	/**
	 * Gets the number of padding bytes.
	 * <p>
	 * On the server, this is always 0. This is only managed on the client and then transferred inside the
	 * encrypted DTO (in {@link CryptoRepoFile#getRepoFileDtoData() CryptoRepoFile.repoFileDtoData}) to the server.
	 * <p>
	 * <b>Important:</b> The chunk's length is updated by CSX to include this padding!
	 * @return the number of padding bytes.
	 */
	public int getPaddingLength() {
		return paddingLength;
	}

	public void setPaddingLength(int paddingLength) {
		this.paddingLength = paddingLength;
	}

	@Override
	public void makeWritable() {
		super.makeWritable();
	}
}
