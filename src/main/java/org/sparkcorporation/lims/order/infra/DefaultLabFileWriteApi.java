package org.sparkcorporation.lims.order.infra;

import java.math.BigDecimal;
import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileWriteApi;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.UpdateLabFile;

@Service
@RequiredArgsConstructor
class DefaultLabFileWriteApi implements LabFileWriteApi {

    private final LabFileRepository labFileRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void setHomeCollectionFee(LabFileId labFileId, BigDecimal amount, UserId recordedBy) {
        LabFile labFile =
                labFileRepository
                        .findActiveById(labFileId.value())
                        .orElseThrow(() -> new LabFileNotFoundException(labFileId));
        labFile.update(
                new UpdateLabFile(null, null, null, null, null, amount, null, null, null), clock);
    }
}
