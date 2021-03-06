package org.subshare.gui.serverlist;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.subshare.core.server.Server;
import org.subshare.core.sync.SyncState;

import co.codewizards.cloudstore.core.Severity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;

public class ServerListItem {

	private final Server server;
	private SyncState pgpSyncState;
	private SyncState lockerSyncState;

	private final StringProperty name;
	private final ObjectProperty<URL> url;
	private final ObjectProperty<Severity> severity = new SimpleObjectProperty<>(this, "severity");

	@SuppressWarnings("unchecked")
	public ServerListItem(final Server server) {
		this.server = requireNonNull(server, "server");
		try {
			name = JavaBeanStringPropertyBuilder.create()
					.bean(server)
					.name(Server.PropertyEnum.name.name()).build();

			url = JavaBeanObjectPropertyBuilder.create()
					.bean(server)
					.name(Server.PropertyEnum.url.name()).build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		updateSeverity();
	}

	public Server getServer() {
		return server;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public ObjectProperty<URL> urlProperty() {
		return url;
	}

	public ObjectProperty<Severity> severityProperty() {
		return severity;
	}

	private void updateSeverity() {
		// TODO get the highest severity of all - once there are more. The repo-sync-state is still missing ;-)
		final SyncState pgpSyncState = getPgpSyncState();
		final SyncState lockerSyncState = getLockerSyncState();

		severity.set(getHighestSeverity(
				(pgpSyncState == null ? null : pgpSyncState.getSeverity()),
				(lockerSyncState == null ? null : lockerSyncState.getSeverity())
				));
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
		updateSeverity();
	}

	public SyncState getLockerSyncState() {
		return lockerSyncState;
	}
	public void setLockerSyncState(SyncState lockerSyncState) {
		this.lockerSyncState = lockerSyncState;
		updateSeverity();
	}

	public String getTooltipText() {
		return assembleTooltipText(
				getPgpSyncStateTooltipText(),
				getLockerSyncStateTooltipText());
	}

	private String assembleTooltipText(final String ... tooltipTexts) {
		requireNonNull(tooltipTexts, "tooltipTexts");

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
		requireNonNull(strings, "strings");
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
