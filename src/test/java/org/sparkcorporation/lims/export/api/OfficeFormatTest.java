package org.sparkcorporation.lims.export.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OfficeFormatTest {

    @Test
    void docx_carries_word_mime_and_extension() {
        assertThat(OfficeFormat.DOCX.mimeType())
                .isEqualTo(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(OfficeFormat.DOCX.extension()).isEqualTo("docx");
    }

    @Test
    void xlsx_carries_excel_mime_and_extension() {
        assertThat(OfficeFormat.XLSX.mimeType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(OfficeFormat.XLSX.extension()).isEqualTo("xlsx");
    }
}
