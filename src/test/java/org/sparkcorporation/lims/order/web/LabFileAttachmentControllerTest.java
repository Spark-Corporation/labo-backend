package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.attachment.api.AttachableType;
import org.sparkcorporation.lims.attachment.api.AttachmentApi;
import org.sparkcorporation.lims.attachment.api.AttachmentId;
import org.sparkcorporation.lims.attachment.api.AttachmentNotFoundException;
import org.sparkcorporation.lims.attachment.api.AttachmentSummary;
import org.sparkcorporation.lims.attachment.api.AttachmentUpload;
import org.sparkcorporation.lims.attachment.api.RenderedAttachment;
import org.sparkcorporation.lims.attachment.web.AttachmentProblemDetailAdvice;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(LabFileAttachmentController.class)
@Import({ProblemDetailAdvice.class, AttachmentProblemDetailAdvice.class})
@WithMockUser(
        username = LabFileAttachmentControllerTest.CALLER,
        authorities = {"lab-file.create", "lab-file.read"})
class LabFileAttachmentControllerTest {

    static final String CALLER = "01HZX0000000000000000VSR01";
    private static final String LAB_FILE = "01HZX00000000000000001BF01";
    private static final String ATT = "01HZX0000000000000000ATT01";
    private static final Instant NOW = Instant.parse("2026-06-04T09:00:00Z");
    private static final int SHA_HEX_PAIRS = 32;

    @Autowired private MockMvc mvc;
    @MockitoBean private AttachmentApi attachmentApi;
    @MockitoBean private LabFileApi labFileApi;

    private static AttachmentSummary summary() {
        return new AttachmentSummary(
                ATT,
                AttachableType.LAB_FILE,
                LAB_FILE,
                "scan.pdf",
                "application/pdf",
                18L,
                "ab".repeat(SHA_HEX_PAIRS),
                "file:///fake/AA/BB/x",
                CALLER,
                NOW);
    }

    @Test
    void uploadReturns201WithSummary() throws Exception {
        when(labFileApi.existsById(new LabFileId(LAB_FILE))).thenReturn(true);
        when(attachmentApi.upload(any())).thenReturn(summary());
        var file =
                new MockMultipartFile(
                        "file", "scan.pdf", "application/pdf", new byte[] {'%', 'P', 'D', 'F'});

        mvc.perform(multipart("/api/v1/lab-files/{id}/attachments", LAB_FILE).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ATT))
                .andExpect(jsonPath("$.filename").value("scan.pdf"));

        var captor = ArgumentCaptor.forClass(AttachmentUpload.class);
        verify(attachmentApi).upload(captor.capture());
        var cmd = captor.getValue();
        assertThat(cmd.type()).isEqualTo(AttachableType.LAB_FILE);
        assertThat(cmd.targetId()).isEqualTo(LAB_FILE);
        assertThat(cmd.uploadedByUserId()).isEqualTo(CALLER);
    }

    @Test
    void uploadToUnknownLabFileReturns404() throws Exception {
        when(labFileApi.existsById(new LabFileId(LAB_FILE))).thenReturn(false);
        var file = new MockMultipartFile("file", "scan.pdf", "application/pdf", new byte[] {1});

        mvc.perform(multipart("/api/v1/lab-files/{id}/attachments", LAB_FILE).file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsSummaries() throws Exception {
        when(labFileApi.existsById(new LabFileId(LAB_FILE))).thenReturn(true);
        when(attachmentApi.list(AttachableType.LAB_FILE, LAB_FILE)).thenReturn(List.of(summary()));

        mvc.perform(get("/api/v1/lab-files/{id}/attachments", LAB_FILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("scan.pdf"));
    }

    @Test
    void downloadStreamsBytesWithStoredTypeAndDisposition() throws Exception {
        when(labFileApi.existsById(new LabFileId(LAB_FILE))).thenReturn(true);
        when(attachmentApi.get(new AttachmentId(ATT)))
                .thenReturn(
                        new RenderedAttachment(
                                new byte[] {'%', 'P', 'D', 'F'}, "application/pdf", "scan.pdf"));

        mvc.perform(get("/api/v1/lab-files/{id}/attachments/{att}", LAB_FILE, ATT))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"scan.pdf\""));
    }

    @Test
    void downloadUnknownAttachmentReturns404() throws Exception {
        when(labFileApi.existsById(new LabFileId(LAB_FILE))).thenReturn(true);
        when(attachmentApi.get(new AttachmentId(ATT)))
                .thenThrow(new AttachmentNotFoundException(new AttachmentId(ATT)));

        mvc.perform(get("/api/v1/lab-files/{id}/attachments/{att}", LAB_FILE, ATT))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        when(labFileApi.existsById(new LabFileId(LAB_FILE))).thenReturn(true);

        mvc.perform(delete("/api/v1/lab-files/{id}/attachments/{att}", LAB_FILE, ATT))
                .andExpect(status().isNoContent());

        verify(attachmentApi).softDelete(new AttachmentId(ATT));
    }
}
