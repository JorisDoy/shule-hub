package ke.shulehub.payments.internal;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Safaricom Daraja request and response bodies. Field names follow Safaricom's PascalCase. */
public final class Daraja {

    private Daraja() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(@JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") String expiresIn) {
    }

    public record StkPushRequest(
            @JsonProperty("BusinessShortCode") String businessShortCode,
            @JsonProperty("Password") String password,
            @JsonProperty("Timestamp") String timestamp,
            @JsonProperty("TransactionType") String transactionType,
            @JsonProperty("Amount") long amount,
            @JsonProperty("PartyA") String partyA,
            @JsonProperty("PartyB") String partyB,
            @JsonProperty("PhoneNumber") String phoneNumber,
            @JsonProperty("CallBackURL") String callBackUrl,
            @JsonProperty("AccountReference") String accountReference,
            @JsonProperty("TransactionDesc") String transactionDesc) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StkPushResponse(
            @JsonProperty("MerchantRequestID") String merchantRequestId,
            @JsonProperty("CheckoutRequestID") String checkoutRequestId,
            @JsonProperty("ResponseCode") String responseCode,
            @JsonProperty("ResponseDescription") String responseDescription,
            @JsonProperty("CustomerMessage") String customerMessage) {
    }

    public record StkQueryRequest(
            @JsonProperty("BusinessShortCode") String businessShortCode,
            @JsonProperty("Password") String password,
            @JsonProperty("Timestamp") String timestamp,
            @JsonProperty("CheckoutRequestID") String checkoutRequestId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StkQueryResponse(
            @JsonProperty("ResponseCode") String responseCode,
            @JsonProperty("ResultCode") String resultCode,
            @JsonProperty("ResultDesc") String resultDesc) {
    }

    public record RegisterUrlRequest(
            @JsonProperty("ShortCode") String shortCode,
            @JsonProperty("ResponseType") String responseType,
            @JsonProperty("ConfirmationURL") String confirmationUrl,
            @JsonProperty("ValidationURL") String validationUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegisterUrlResponse(
            @JsonProperty("ResponseCode") String responseCode,
            @JsonProperty("ResponseDescription") String responseDescription) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorResponse(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("errorCode") String errorCode,
            @JsonProperty("errorMessage") String errorMessage) {
    }

    // ---- Callbacks Safaricom sends to us ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StkCallbackEnvelope(@JsonProperty("Body") StkCallbackBody body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StkCallbackBody(@JsonProperty("stkCallback") StkCallback stkCallback) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StkCallback(
            @JsonProperty("MerchantRequestID") String merchantRequestId,
            @JsonProperty("CheckoutRequestID") String checkoutRequestId,
            @JsonProperty("ResultCode") Integer resultCode,
            @JsonProperty("ResultDesc") String resultDesc,
            @JsonProperty("CallbackMetadata") CallbackMetadata callbackMetadata) {

        /** The value of a metadata item such as "MpesaReceiptNumber" or "Amount", as text. */
        public String item(String name) {
            if (callbackMetadata == null || callbackMetadata.items() == null) {
                return null;
            }
            return callbackMetadata.items().stream()
                    .filter(item -> name.equals(item.name()) && item.value() != null)
                    // Integers (dates, phones) arrive as Integer/Long and amounts as decimals; all print plainly.
                    .map(item -> String.valueOf(item.value()))
                    .findFirst()
                    .orElse(null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallbackMetadata(@JsonProperty("Item") List<CallbackItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallbackItem(@JsonProperty("Name") String name, @JsonProperty("Value") Object value) {
    }

    /** Body of C2B validation and confirmation callbacks. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record C2bPayload(
            @JsonProperty("TransactionType") String transactionType,
            @JsonProperty("TransID") String transId,
            @JsonProperty("TransTime") String transTime,
            @JsonProperty("TransAmount") String transAmount,
            @JsonProperty("BusinessShortCode") String businessShortCode,
            @JsonProperty("BillRefNumber") String billRefNumber,
            @JsonProperty("InvoiceNumber") String invoiceNumber,
            @JsonProperty("MSISDN") String msisdn,
            @JsonProperty("FirstName") String firstName,
            @JsonProperty("MiddleName") String middleName,
            @JsonProperty("LastName") String lastName) {

        public String payerName() {
            StringBuilder name = new StringBuilder();
            for (String part : new String[] { firstName, middleName, lastName }) {
                if (part != null && !part.isBlank()) {
                    if (!name.isEmpty()) {
                        name.append(' ');
                    }
                    name.append(part.trim());
                }
            }
            return name.isEmpty() ? null : name.toString();
        }
    }

    /** What we answer to C2B callbacks: "0" accepts, "C2B00012" rejects an invalid account number. */
    public record C2bResponse(@JsonProperty("ResultCode") String resultCode, @JsonProperty("ResultDesc") String resultDesc) {

        public static C2bResponse accept() {
            return new C2bResponse("0", "Accepted");
        }

        public static C2bResponse rejectInvalidAccount() {
            return new C2bResponse("C2B00012", "Rejected");
        }
    }

    /** What we answer to STK callbacks. */
    public record CallbackAck(@JsonProperty("ResultCode") int resultCode, @JsonProperty("ResultDesc") String resultDesc) {

        public static CallbackAck ok() {
            return new CallbackAck(0, "Accepted");
        }
    }
}
