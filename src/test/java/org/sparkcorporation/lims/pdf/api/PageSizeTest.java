package org.sparkcorporation.lims.pdf.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageSizeTest {

    @Test
    void eachEnumExposesWidthAndHeightInMm() {
        assertThat(PageSize.A4_PORTRAIT.widthMm()).isEqualTo(210f);
        assertThat(PageSize.A4_PORTRAIT.heightMm()).isEqualTo(297f);

        assertThat(PageSize.A4_LANDSCAPE.widthMm()).isEqualTo(297f);
        assertThat(PageSize.A4_LANDSCAPE.heightMm()).isEqualTo(210f);

        assertThat(PageSize.A5_PORTRAIT.widthMm()).isEqualTo(148f);
        assertThat(PageSize.A5_PORTRAIT.heightMm()).isEqualTo(210f);
    }
}
