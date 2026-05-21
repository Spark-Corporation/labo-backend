package org.sparkcorporation.lims.pdf.infra;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.PageSizeUnits;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.spring6.SpringTemplateEngine;

import org.sparkcorporation.lims.pdf.api.PdfDocumentType;
import org.sparkcorporation.lims.pdf.api.PdfEngineException;
import org.sparkcorporation.lims.pdf.api.PdfModelException;
import org.sparkcorporation.lims.pdf.api.PdfRenderJobQueued;
import org.sparkcorporation.lims.pdf.api.PdfRenderJobView;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.pdf.api.PdfTemplateException;
import org.sparkcorporation.lims.pdf.api.RenderedPdf;
import org.sparkcorporation.lims.shared.id.Ulid;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenHtmlToPdfRenderingService implements PdfRenderingService {

    private final SpringTemplateEngine templateEngine;
    private final PdfFontRegistry fontRegistry;
    private final PdfMetrics metrics;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final PdfRenderJobRepository jobRepo;
    private final ObjectMapper objectMapper;

    @Override
    public <T> RenderedPdf render(PdfDocumentType<T> docType, T model) {
        var sample = metrics.startTimer();
        MDC.put("pdfDocumentType", docType.id());
        try {
            var html = renderTemplate(docType, model);
            var bytes = renderPdf(docType, html);
            return new RenderedPdf(bytes, docType.defaultFilename(model), Instant.now(clock));
        } finally {
            metrics.stopTimerSync(sample, docType.id());
            MDC.remove("pdfDocumentType");
        }
    }

    private <T> String renderTemplate(PdfDocumentType<T> docType, T model) {
        try {
            var ctx = new Context();
            ctx.setVariables(BeanModelIntrospector.toMap(model));
            return templateEngine.process(docType.templatePath(), ctx);
        } catch (TemplateInputException ex) {
            metrics.incrementFailure(docType.id(), "template_not_found");
            throw new PdfTemplateException(docType.id(), "template_not_found", ex);
        } catch (RuntimeException ex) {
            metrics.incrementFailure(docType.id(), "template_parse_error");
            throw new PdfTemplateException(docType.id(), "template_parse_error", ex);
        }
    }

    private <T> byte[] renderPdf(PdfDocumentType<T> docType, String html) {
        try (var out = new ByteArrayOutputStream()) {
            var builder =
                    new PdfRendererBuilder()
                            .useFastMode()
                            .useDefaultPageSize(
                                    docType.pageSize().widthMm(),
                                    docType.pageSize().heightMm(),
                                    PageSizeUnits.MM)
                            .withHtmlContent(html, "classpath:/templates/")
                            .toStream(out);
            fontRegistry.applyTo(builder);
            builder.run();
            return out.toByteArray();
        } catch (IOException ex) {
            metrics.incrementFailure(docType.id(), "io_error");
            throw new PdfEngineException(docType.id(), "io_error", ex);
        } catch (RuntimeException ex) {
            metrics.incrementFailure(docType.id(), "unknown");
            throw new PdfEngineException(docType.id(), "unknown", ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> String enqueue(PdfDocumentType<T> docType, T model) {
        var jobId = Ulid.generate();
        String modelJson;
        try {
            modelJson = objectMapper.writeValueAsString(model);
        } catch (JsonProcessingException ex) {
            metrics.incrementFailure(docType.id(), "model_invalid");
            throw new PdfModelException(docType.id(), "model_invalid", ex);
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String userId =
                (auth != null
                                && auth.isAuthenticated()
                                && !"anonymousUser".equals(auth.getPrincipal()))
                        ? auth.getName()
                        : null;
        var now = Instant.now(clock);
        try {
            jobRepo.save(PdfRenderJob.queued(jobId, docType.id(), modelJson, userId, now));
            eventPublisher.publishEvent(new PdfRenderJobQueued(jobId, docType.id(), userId, now));
            return jobId;
        } catch (DataAccessException ex) {
            metrics.incrementFailure(docType.id(), "io_error");
            throw new PdfEngineException(docType.id(), "io_error", ex);
        }
    }

    @Override
    public Optional<PdfRenderJobView> findJob(String jobId) {
        return jobRepo.findById(jobId).map(PdfJobResponseMapper::toView);
    }

    @Override
    public Optional<String> findJobOwner(String jobId) {
        return jobRepo.findById(jobId).map(PdfRenderJob::getRequestedByUserId);
    }
}
