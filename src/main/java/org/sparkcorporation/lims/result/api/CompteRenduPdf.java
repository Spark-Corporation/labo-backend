package org.sparkcorporation.lims.result.api;

/**
 * A rendered compte-rendu PDF, owned by {@code result} so consumers need no {@code pdf} dependency.
 */
public record CompteRenduPdf(byte[] bytes, String filename) {}
