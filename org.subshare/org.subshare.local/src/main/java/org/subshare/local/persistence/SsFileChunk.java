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

	@Column(defaultValue = "-1")
	private int lengthWithPadding = -1;

	/**
	 * Gets the length of the chunk with padding bytes.
	 * <p>
	 * On the server, this is always -1. This is only managed on the client and then transferred inside the
	 * encrypted DTO (in {@link CryptoRepoFile#getRepoFileDtoData() CryptoRepoFile.repoFileDtoData}) to the server.
	 * <p>
	 * {@link #getLength() length} does not include the padding on the client-side - on the server-side, however,
	 * {@code length} includes the padding.
	 * @return the length of the chunk including both payload and padding bytes. Always -1 on the server-side.
	 */
	public int getLengthWithPadding() {
		return lengthWithPadding;
	}

	public void setLengthWithPadding(int lengthWithPadding) {
		this.lengthWithPadding = lengthWithPadding;
	}

	@Override
	public void makeWritable() {
		super.makeWritable();
	}
}
