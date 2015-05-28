package org.subshare.rest.client.locker.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.List;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.dto.ListDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetLockerContentVersions extends AbstractRequest<List<Uid>> {

	private final PgpKeyId pgpKeyId;
	private final String lockerContentName;

	public GetLockerContentVersions(final PgpKeyId pgpKeyId, final String lockerContentName) {
		this.pgpKeyId = assertNotNull("pgpKeyId", pgpKeyId);
		this.lockerContentName = assertNotNull("lockerContentName", lockerContentName);
	}

	@Override
	public List<Uid> execute() {
		final WebTarget webTarget = createWebTarget("_Locker", urlEncode(pgpKeyId.toString()), urlEncode(lockerContentName));
		@SuppressWarnings("unchecked")
		final ListDto<Uid> listDto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML_TYPE)).get(ListDto.class);
		return listDto.getElements();
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
