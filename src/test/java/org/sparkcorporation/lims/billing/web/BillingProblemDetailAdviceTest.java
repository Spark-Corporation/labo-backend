package org.sparkcorporation.lims.billing.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.domain.CashRegisterInvalidUserReferenceException;
import org.sparkcorporation.lims.billing.domain.CashRegisterNotAssignedException;
import org.sparkcorporation.lims.billing.domain.InvalidPricingException;
import org.sparkcorporation.lims.billing.domain.InvoiceAlreadyExistsForLabFileException;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidCashRegisterReferenceException;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidLabFileReferenceException;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Verifies the 6 {@code @ExceptionHandler} methods of {@link BillingProblemDetailAdvice} per spec
 * §7.5. Each handler is exercised via a throwing controller endpoint hit by {@link MockMvc}; the
 * resulting {@link org.springframework.http.ProblemDetail} payload is asserted shape-by-shape.
 */
@ExtendWith(SpringExtension.class)
class BillingProblemDetailAdviceTest {

    private static final String LAB_FILE_ULID = Ulid.generate();
    private static final String INVOICE_ULID = Ulid.generate();
    private static final String USER_ULID = Ulid.generate();
    private static final String CASH_REGISTER_ULID = Ulid.generate();

    @RestController
    @RequestMapping("/test-billing-throws")
    static class ThrowingController {

        @GetMapping("/invoice-already-exists")
        public String invoiceAlreadyExists() {
            throw new InvoiceAlreadyExistsForLabFileException(
                    new LabFileId(LAB_FILE_ULID), new InvoiceId(INVOICE_ULID));
        }

        @GetMapping("/invalid-pricing")
        public String invalidPricing() {
            throw new InvalidPricingException("net amount must be >= 0");
        }

        @GetMapping("/cash-register-invalid-user")
        public String cashRegisterInvalidUser() {
            throw new CashRegisterInvalidUserReferenceException(new UserId(USER_ULID));
        }

        @GetMapping("/payment-invalid-lab-file")
        public String paymentInvalidLabFile() {
            throw new PaymentInvalidLabFileReferenceException(LAB_FILE_ULID);
        }

        @GetMapping("/payment-invalid-cash-register")
        public String paymentInvalidCashRegister() {
            throw new PaymentInvalidCashRegisterReferenceException(CASH_REGISTER_ULID);
        }

        @GetMapping("/cash-register-not-assigned")
        public String cashRegisterNotAssigned() {
            throw new CashRegisterNotAssignedException(CASH_REGISTER_ULID, USER_ULID);
        }
    }

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ThrowingController())
                    .setControllerAdvice(new BillingProblemDetailAdvice())
                    .build();

    @Test
    void invoiceAlreadyExists_returns_409_with_labFileId_and_existingInvoiceId() throws Exception {
        mockMvc.perform(get("/test-billing-throws/invoice-already-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invoice already issued for lab file"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("/problems/invoice-already-exists"))
                .andExpect(jsonPath("$.labFileId").value(LAB_FILE_ULID))
                .andExpect(jsonPath("$.existingInvoiceId").value(INVOICE_ULID))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void invalidPricing_returns_422_problem_detail() throws Exception {
        mockMvc.perform(get("/test-billing-throws/invalid-pricing"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid pricing"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/problems/invalid-pricing"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void cashRegisterInvalidUser_returns_404_with_userId_property() throws Exception {
        mockMvc.perform(get("/test-billing-throws/cash-register-invalid-user"))
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Cash-register user assignment references unknown user"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("/problems/cash-register-invalid-user"))
                .andExpect(jsonPath("$.userId").value(USER_ULID))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void paymentInvalidLabFile_returns_404_with_labFileId_property() throws Exception {
        mockMvc.perform(get("/test-billing-throws/payment-invalid-lab-file"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Payment references unknown lab file"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("/problems/payment-invalid-lab-file"))
                .andExpect(jsonPath("$.labFileId").value(LAB_FILE_ULID))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void paymentInvalidCashRegister_returns_404_with_cashRegisterId_property() throws Exception {
        mockMvc.perform(get("/test-billing-throws/payment-invalid-cash-register"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Payment references unknown cash register"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("/problems/payment-invalid-cash-register"))
                .andExpect(jsonPath("$.cashRegisterId").value(CASH_REGISTER_ULID))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void cashRegisterNotAssigned_returns_403_with_cashRegisterId_and_userId() throws Exception {
        mockMvc.perform(get("/test-billing-throws/cash-register-not-assigned"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Calling user not assigned to cash register"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.type").value("/problems/cash-register-not-assigned"))
                .andExpect(jsonPath("$.cashRegisterId").value(CASH_REGISTER_ULID))
                .andExpect(jsonPath("$.userId").value(USER_ULID))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
