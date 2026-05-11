package org.sparkcorporation.lims.result.domain;

import org.jspecify.annotations.Nullable;

/**
 * Command record for {@link Result#setLineValue}.
 *
 * <p>Both fields are independently optional at the controller layer: a {@code null} value clears
 * the line; a {@code null} {@code commentText} leaves the existing comment untouched; an empty
 * {@code commentText} clears the existing comment.
 */
public record NewLineValue(@Nullable ResultValue value, @Nullable String commentText) {}
