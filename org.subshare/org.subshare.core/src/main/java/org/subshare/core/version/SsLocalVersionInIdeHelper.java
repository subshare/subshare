package org.subshare.core.version;

import static co.codewizards.cloudstore.core.util.UrlUtil.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.version.LocalVersionInIdeHelper;
import co.codewizards.cloudstore.core.version.Version;

public class SsLocalVersionInIdeHelper extends LocalVersionInIdeHelper {

	protected SsLocalVersionInIdeHelper() {
	}

	@Override
	protected Version getLocalVersionInIde_file() {
		try {
			File dir = getFile(resource).getCanonicalFile();
			List<File> buildGradleFiles = new ArrayList<File>();
			while (true) {
				dir = dir.getParentFile();
				if (dir == null)
					break;

				File buildGradleFile = dir.createFile("build.gradle");
				if (buildGradleFile.exists())
					buildGradleFiles.add(buildGradleFile);
			};

			for (File buildGradleFile : buildGradleFiles) {
				try (InputStream buildGradleIn = buildGradleFile.createInputStream()) {
					Version version = readVersionFromBuildGradle(buildGradleIn);
					if (version != null)
						return version;
				}
			}
			throw new IllegalStateException("Could not determine local version!");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected Version readVersionFromBuildGradle(InputStream buildGradleIn) throws IOException {
		// quick'n'dirty implementation
		final Pattern pattern = Pattern.compile("^\\s*version\\s*=\\s*'([^']*)'.*$");
		BufferedReader reader = new BufferedReader(new InputStreamReader(buildGradleIn, StandardCharsets.UTF_8));
		Matcher matcher = null;
		String line;
		while (null != (line = reader.readLine())) {
			if (matcher == null)
				matcher = pattern.matcher(line);
			else
				matcher.reset(line);

			if (matcher.matches()) {
				String versionString = matcher.group(1);
				return new Version(versionString);
			}
		}
		return null;
	}

	@Override
	protected Version getLocalVersionInIde_jar() {
		resource = getFileUrlFromJarUrl(resource);
		// We need to read the root-build.gradle, anyway. This should be findable, if we start from
		// the URL pointing to the JAR.
		return getLocalVersionInIde_file();
	}

}
