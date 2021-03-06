package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An enumeration of possible provider types for mapping definitions.
 */
public enum ProviderType {

	JSON_LD_PACKAGE,

	ILCD_PACKAGE,

	CSV_FILE,

	UNKNOWN;

	/**
	 * Try to determine the provider type from the given file.
	 */
	public static ProviderType of(File file) {
		Logger log = LoggerFactory.getLogger(ProviderType.class);
		log.trace("Try to determine provider type of {}", file);

		if (file == null || !file.isFile()) {
			log.error("Given thing is not a file");
			return UNKNOWN;
		}

		String fname = file.getName().toLowerCase();
		if (fname.endsWith(".csv")) {
			log.trace("Found a CSV file; check the format");
			// TODO: we should check the format here
			return CSV_FILE;
		}

		if (!fname.endsWith(".zip"))
			return UNKNOWN;

		try (ZipFile zf = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zf.entries();
			while (entries.hasMoreElements()) {
				ZipEntry ze = entries.nextElement();
				String zname = ze.getName();
				if (zname.matches(".*flows[\\\\|\\/].*\\.json")) {
					log.trace("found a flows/*.json file; "
							+ "assuming the format is JSON-LD");
					return JSON_LD_PACKAGE;
				}
				if (zname.matches(".*flows[\\\\|\\/].*\\.xml")) {
					log.trace("found a flows/*.xml file; "
							+ "assuming the format is ILCD");
					return ILCD_PACKAGE;
				}
			}
			log.info("No flows found in {}; "
					+ "-> unknown mapping format", file);
			return UNKNOWN;
		} catch (Exception e) {
			log.error("Failed to open file " + file, e);
			return UNKNOWN;
		}
	}
}