package org.sparkcorporation.lims.cashbox.web;

import org.jspecify.annotations.Nullable;

/**
 * Multipart payload carrier so the cashbox attachment services stay servlet-free and unit-testable
 * without servlet mocks. Shared by the expense and home-collection-fee attachment services.
 */
public record UploadedScan(String filename, @Nullable String declaredContentType, byte[] bytes) {}
