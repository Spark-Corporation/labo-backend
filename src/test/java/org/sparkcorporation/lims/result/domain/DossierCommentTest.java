package org.sparkcorporation.lims.result.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.LabFileId;

class DossierCommentTest {

    private static final LabFileId LF = new LabFileId("01KQZ0WPVQA364NMRAJMKRQC84");

    @Test
    void create_then_update_replaces_commentText() {
        var dc = DossierComment.create(LF, "first comment");
        assertThat(dc.getCommentText()).isEqualTo("first comment");

        dc.update("second comment");

        assertThat(dc.getCommentText()).isEqualTo("second comment");
    }

    @Test
    void update_does_not_change_labFileId() {
        var dc = DossierComment.create(LF, "first");

        dc.update("revised");

        assertThat(dc.getLabFileId()).isEqualTo(LF.value());
    }
}
