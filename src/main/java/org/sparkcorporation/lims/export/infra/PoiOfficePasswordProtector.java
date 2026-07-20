package org.sparkcorporation.lims.export.infra;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.export.api.OfficePasswordProtector;
import org.sparkcorporation.lims.export.api.OfficeRenderException;

/** POI agile (AES) OOXML encryption — works identically for .docx and .xlsx. */
@Component
public class PoiOfficePasswordProtector implements OfficePasswordProtector {

    @Override
    public byte[] protect(byte[] ooxml, String password) {
        try (var fs = new POIFSFileSystem()) {
            var info = new EncryptionInfo(EncryptionMode.agile);
            Encryptor enc = info.getEncryptor();
            enc.confirmPassword(password);
            try (OPCPackage opc = OPCPackage.open(new ByteArrayInputStream(ooxml));
                    OutputStream os = enc.getDataStream(fs)) {
                opc.save(os);
            }
            try (var bos = new ByteArrayOutputStream()) {
                fs.writeFilesystem(bos);
                return bos.toByteArray();
            }
        } catch (Exception ex) {
            throw new OfficeRenderException("office.encrypt", "encrypt_failed", ex);
        }
    }
}
