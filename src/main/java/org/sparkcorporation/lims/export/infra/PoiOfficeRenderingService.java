package org.sparkcorporation.lims.export.infra;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.sparkcorporation.lims.export.api.OfficeDocumentType;
import org.sparkcorporation.lims.export.api.OfficeRenderException;
import org.sparkcorporation.lims.export.api.OfficeRenderingService;
import org.sparkcorporation.lims.export.api.RenderedOffice;

/** POI-backed {@link OfficeRenderingService}. Thin: delegates to {@code build}, stamps, wraps. */
@Service
@RequiredArgsConstructor
public class PoiOfficeRenderingService implements OfficeRenderingService {

    private final Clock clock;

    @Override
    public <T> RenderedOffice render(OfficeDocumentType<T> docType, T model) {
        try {
            byte[] bytes = docType.build(model);
            return new RenderedOffice(
                    bytes, docType.defaultFilename(model), docType.format(), clock.instant());
        } catch (RuntimeException ex) {
            throw new OfficeRenderException(docType.id(), "build_failed", ex);
        }
    }
}
