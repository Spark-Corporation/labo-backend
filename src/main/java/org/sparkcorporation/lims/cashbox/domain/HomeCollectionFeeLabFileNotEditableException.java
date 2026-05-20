package org.sparkcorporation.lims.cashbox.domain;

/**
 * Cashbox-side wrapper around {@code order.domain.LabFileNotEditableException}, raised by {@code
 * HomeCollectionFeeService} when {@code LabFileWriteApi.setHomeCollectionFee} rejects the update
 * because the target lab file is not {@code OPEN}. Kept inside {@code cashbox::domain} so the
 * cashbox controller-advice can map it without taking a forbidden dependency on {@code
 * order::domain} (Spring Modulith boundary).
 *
 * <p>Mapped to HTTP 409 by {@code CashboxProblemDetailAdvice}.
 */
public class HomeCollectionFeeLabFileNotEditableException extends RuntimeException {

    public HomeCollectionFeeLabFileNotEditableException(String labFileId, Throwable cause) {
        super("lab file is not editable: " + labFileId, cause);
    }
}
