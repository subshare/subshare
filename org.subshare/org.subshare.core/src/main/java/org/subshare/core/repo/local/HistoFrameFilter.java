package org.subshare.core.repo.local;

import java.io.Serializable;
import java.util.Date;

import org.subshare.core.sign.Signature;

@SuppressWarnings("serial")
public class HistoFrameFilter implements Serializable {

	private int maxResultSize = -1;

	private Date signatureCreatedFrom;
	private Date signatureCreatedTo;

	/**
	 * Gets the maximum size of the query result (a {@code Collection<HistoFrameDto>}).
	 * The newest entries are included (and older entries omitted).
	 * <p>
	 * For example, if this value is 200, only the newest 200 {@code HistoFrameDto}s
	 * matching all other criteria are returned.
	 * <p>
	 * A value {@code <= 0} means not to restrict the result's size.
	 * @return the maximum size of the {@code Collection<HistoFrameDto>} being the result of the query.
	 */
	public int getMaxResultSize() {
		return maxResultSize;
	}
	public void setMaxResultSize(int maxResultListSize) {
		this.maxResultSize = maxResultListSize;
	}

	/**
	 * Gets the beginning (included) of the time range filtering the {@link Signature#getSignatureCreated() signatureCreated}.
	 * <p>
	 * A value of <code>null</code> means not to restrict the time range on the "from" side.
	 * @return the beginning (included) of the time range filtering the {@link Signature#getSignatureCreated() signatureCreated}.
	 * May be <code>null</code> to not filter by this criterion.
	 */
	public Date getSignatureCreatedFrom() {
		return signatureCreatedFrom;
	}
	public void setSignatureCreatedFrom(Date signatureCreatedFrom) {
		this.signatureCreatedFrom = signatureCreatedFrom;
	}

	/**
	 * Gets the end (excluded) of the time range filtering the {@link Signature#getSignatureCreated() signatureCreated}.
	 * <p>
	 * A value of <code>null</code> means not to restrict the time range on the "to"/"until" side.
	 * @return the end (excluded) of the time range filtering the {@link Signature#getSignatureCreated() signatureCreated}.
	 * May be <code>null</code> to not filter by this criterion.
	 */
	public Date getSignatureCreatedTo() {
		return signatureCreatedTo;
	}
	public void setSignatureCreatedTo(Date signatureCreatedTo) {
		this.signatureCreatedTo = signatureCreatedTo;
	}
}
