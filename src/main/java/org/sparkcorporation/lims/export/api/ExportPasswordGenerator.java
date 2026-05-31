package org.sparkcorporation.lims.export.api;

/**
 * Generates a human-relayable random password for protected exports.
 *
 * <p><strong>The returned password is a secret — never log it.</strong>
 */
public interface ExportPasswordGenerator {

    /** Returns a new random password suitable for protecting an exported document. */
    String generate();
}
