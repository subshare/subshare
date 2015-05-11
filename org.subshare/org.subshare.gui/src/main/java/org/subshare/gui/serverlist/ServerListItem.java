package org.subshare.gui.serverlist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.subshare.core.Severity;
import org.subshare.core.pgp.sync.PgpSyncState;
import org.subshare.core.server.Server;

public class ServerListItem {

	private Server server;
	private PgpSyncState pgpSyncState;

	public ServerListItem() { }

	public ServerListItem(final Server server) {
		this.server = server;
	}

	public Server getServer() {
		return server;
	}
	public void setServer(Server server) {
		this.server = server;
	}

	public String getName() {
		return assertNotNull("server", server).getName();
	}
	public void setName(String name) {
		assertNotNull("server", server).setName(name);
	}

	public URL getUrl() {
		return assertNotNull("server", server).getUrl();
	}
	public void setUrl(URL url) {
		assertNotNull("server", server).setUrl(url);
	}

	public Severity getSeverity() {
		// TODO get the highest severity of all - once there are more.

		final PgpSyncState pgpSyncState = getPgpSyncState();

		return getHighestSeverity(
				pgpSyncState == null ? null : pgpSyncState.getSeverity()
				);
	}

	private static Severity getHighestSeverity(Severity ... severities) {
		Severity result = Severity.values()[0]; // lowest (as fallback)
		for (final Severity severity : severities) {
			if (severity == null)
				continue;

			if (result.ordinal() < severity.ordinal())
				result = severity;
		}
		return result;
	}

	public PgpSyncState getPgpSyncState() {
		return pgpSyncState;
	}
	public void setPgpSyncState(PgpSyncState pgpSyncState) {
		this.pgpSyncState = pgpSyncState;
	}

	public String getTooltipText() {
		final List<String> tooltipTexts = new ArrayList<>();

		final String pgpSyncStateTooltipText = getPgpSyncStateTooltipText();
		if (!isEmpty(pgpSyncStateTooltipText))
			tooltipTexts.add(pgpSyncStateTooltipText);

		return assembleTooltipText(tooltipTexts);
	}

	private String assembleTooltipText(final List<String> tooltipTexts) {
		if (tooltipTexts.isEmpty())
			return null;
		else if (tooltipTexts.size() == 1)
			return tooltipTexts.get(0);
		else {
			final StringBuilder sb = new StringBuilder();
			for (final String tooltipText : tooltipTexts) {
				if (sb.length() > 0)
					sb.append("\n");

				sb.append("* ").append(tooltipText);
			}
			return sb.toString();
		}
	}

	private String getPgpSyncStateTooltipText() {
		if (pgpSyncState != null) {
			if (!isEmpty(pgpSyncState.getMessage()))
				return pgpSyncState.getMessage();
			else if (pgpSyncState.getError() != null) {
				if (!isEmpty(pgpSyncState.getError().getMessage()))
					return pgpSyncState.getError().getClassName() + ": " + pgpSyncState.getError().getMessage();
				else
					return pgpSyncState.getError().getClassName();
			}
		}
		return null;
	}
}
