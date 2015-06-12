package org.subshare.gui.serverlist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.subshare.core.Severity;
import org.subshare.core.server.Server;
import org.subshare.core.sync.SyncState;

public class ServerListItem {

	private Server server;
	private SyncState pgpSyncState;
	private SyncState lockerSyncState;

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
		// TODO get the highest severity of all - once there are more. The repo-sync-state is still missing ;-)

		final SyncState pgpSyncState = getPgpSyncState();
		final SyncState lockerSyncState = getLockerSyncState();

		return getHighestSeverity(
				(pgpSyncState == null ? null : pgpSyncState.getSeverity()),
				(lockerSyncState == null ? null : lockerSyncState.getSeverity())
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

	public SyncState getPgpSyncState() {
		return pgpSyncState;
	}
	public void setPgpSyncState(SyncState pgpSyncState) {
		this.pgpSyncState = pgpSyncState;
	}

	public SyncState getLockerSyncState() {
		return lockerSyncState;
	}
	public void setLockerSyncState(SyncState lockerSyncState) {
		this.lockerSyncState = lockerSyncState;
	}

	public String getTooltipText() {
		return assembleTooltipText(
				getPgpSyncStateTooltipText(),
				getLockerSyncStateTooltipText());
	}

	private String assembleTooltipText(final String ... tooltipTexts) {
		assertNotNull("tooltipTexts", tooltipTexts);

		final List<String> tooltipTextList = Arrays.asList(tooltipTexts);
		return assembleTooltipText(tooltipTextList);
	}

	private String assembleTooltipText(List<String> tooltipTexts) {
		tooltipTexts = filterEmpty(tooltipTexts);

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

	private List<String> filterEmpty(final List<String> strings) {
		assertNotNull("strings", strings);
		final List<String> result = new ArrayList<String>(strings.size());
		for (final String string : strings) {
			if (! isEmpty(string))
				result.add(string);
		}
		return result;
	}

	private String getPgpSyncStateTooltipText() {
		return getSyncStateTooltipText(pgpSyncState);
	}

	private String getLockerSyncStateTooltipText() {
		return getSyncStateTooltipText(lockerSyncState);
	}

	private static String getSyncStateTooltipText(final SyncState syncState) {
		if (syncState != null) {
			if (!isEmpty(syncState.getMessage()))
				return syncState.getMessage();

			if (syncState.getError() != null) {
				if (!isEmpty(syncState.getError().getMessage()))
					return syncState.getError().getClassName() + ": " + syncState.getError().getMessage();
				else
					return syncState.getError().getClassName();
			}
		}
		return null;
	}
}
