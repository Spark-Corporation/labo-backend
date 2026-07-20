package org.sparkcorporation.lims.attachment.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttachableTypeTest {

    @Test
    void exposesExactlyTheThreeSpecTargets() {
        assertThat(AttachableType.values())
                .containsExactly(
                        AttachableType.LAB_FILE,
                        AttachableType.EXPENSE,
                        AttachableType.HOME_COLLECTION_FEE);
    }
}
