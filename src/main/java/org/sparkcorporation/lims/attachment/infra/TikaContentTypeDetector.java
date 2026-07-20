package org.sparkcorporation.lims.attachment.infra;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/**
 * Magic-byte content-type detection (spec §5 step 3). Permissive by design (spec decision #4):
 * detection result is stored, never used to reject. The filename is a tie-break hint only — magic
 * bytes win when they disagree (e.g. OOXML containers all share the zip magic).
 */
@Component
public class TikaContentTypeDetector {

    private final Tika tika = new Tika();

    public String detect(byte[] bytes, String filename) {
        return tika.detect(bytes, filename);
    }
}
