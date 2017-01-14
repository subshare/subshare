package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Comparator;

public class CryptoRepoFilePathComparator implements Comparator<CryptoRepoFile> {

	@Override
	public int compare(final CryptoRepoFile crf1, final CryptoRepoFile crf2) {
		if (crf1.equals(crf2))
			return 0;

		if (isParentOrEqual(crf1, crf2))
			return -1;

		if (isParentOrEqual(crf2, crf1))
			return +1;

		throw new IllegalArgumentException("CryptoRepoFiles are not in a path: " + crf1 + ", " + crf2);
	}

	private boolean isParentOrEqual(final CryptoRepoFile parentCandidate, final CryptoRepoFile childCandidate) {
		assertNotNull(parentCandidate, "parentCandidate");
		assertNotNull(childCandidate, "childCandidate");

		CryptoRepoFile crf = childCandidate;
		while (crf != null) {
			if (parentCandidate.equals(crf))
				return true;

			crf = crf.getParent();
		}
		return false;
	}
}
