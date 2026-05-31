package org.sparkcorporation.lims.export.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class PoiOfficePasswordProtectorTest {

    private final PoiOfficePasswordProtector protector = new PoiOfficePasswordProtector();

    private static byte[] tinyDocx() throws Exception {
        try (var doc = new XWPFDocument();
                var bos = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText("hello");
            doc.write(bos);
            return bos.toByteArray();
        }
    }

    private static byte[] tinyXlsx() throws Exception {
        try (var wb = new XSSFWorkbook();
                var bos = new ByteArrayOutputStream()) {
            wb.createSheet("S").createRow(0).createCell(0).setCellValue("hello");
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private static boolean opensWith(byte[] encrypted, String password) throws Exception {
        try (var fs = new POIFSFileSystem(new ByteArrayInputStream(encrypted))) {
            var dec = Decryptor.getInstance(new EncryptionInfo(fs));
            return dec.verifyPassword(password);
        }
    }

    @Test
    void encrypted_docx_opens_only_with_the_password() throws Exception {
        byte[] encrypted = protector.protect(tinyDocx(), "S3CRET");
        assertThat(opensWith(encrypted, "S3CRET")).isTrue();
        assertThat(opensWith(encrypted, "WRONG")).isFalse();
    }

    @Test
    void encrypted_xlsx_opens_only_with_the_password() throws Exception {
        byte[] encrypted = protector.protect(tinyXlsx(), "S3CRET");
        assertThat(opensWith(encrypted, "S3CRET")).isTrue();
        assertThat(opensWith(encrypted, "WRONG")).isFalse();
    }
}
