package ke.shulehub.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class DarajaTests {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void passwordIsBase64OfShortcodePasskeyAndTimestamp() {
        String password = DarajaClient.password("174379", "passkey", "20261020120000");
        assertThat(new String(Base64.getDecoder().decode(password), StandardCharsets.UTF_8))
                .isEqualTo("174379passkey20261020120000");
    }

    @Test
    void readsTheStkCallbackMetadata() {
        String body = """
                {"Body":{"stkCallback":{"MerchantRequestID":"29115-34620561-1","CheckoutRequestID":"ws_CO_191220191020363925",
                "ResultCode":0,"ResultDesc":"The service request is processed successfully.","CallbackMetadata":{"Item":[
                {"Name":"Amount","Value":1.00},{"Name":"MpesaReceiptNumber","Value":"NLJ7RT61SV"},{"Name":"Balance"},
                {"Name":"TransactionDate","Value":20191219102115},{"Name":"PhoneNumber","Value":254708374149}]}}}}
                """;
        Daraja.StkCallback callback = json.readValue(body, Daraja.StkCallbackEnvelope.class).body().stkCallback();

        assertThat(callback.resultCode()).isZero();
        assertThat(callback.item("MpesaReceiptNumber")).isEqualTo("NLJ7RT61SV");
        assertThat(callback.item("TransactionDate")).isEqualTo("20191219102115");
        assertThat(callback.item("PhoneNumber")).isEqualTo("254708374149");
        assertThat(new java.math.BigDecimal(callback.item("Amount"))).isEqualByComparingTo("1");
        assertThat(callback.item("Balance")).isNull();
    }

    @Test
    void readsACancelledStkCallback() {
        String body = """
                {"Body":{"stkCallback":{"MerchantRequestID":"1","CheckoutRequestID":"ws_CO_1","ResultCode":1032,
                "ResultDesc":"Request cancelled by user"}}}
                """;
        Daraja.StkCallback callback = json.readValue(body, Daraja.StkCallbackEnvelope.class).body().stkCallback();
        assertThat(callback.resultCode()).isEqualTo(1032);
        assertThat(callback.item("MpesaReceiptNumber")).isNull();
    }

    @Test
    void readsAC2bConfirmation() {
        String body = """
                {"TransactionType":"Pay Bill","TransID":"RKTQDM7W6S","TransTime":"20191122063845","TransAmount":"10.00",
                 "BusinessShortCode":"600638","BillRefNumber":"rua0042","InvoiceNumber":"","OrgAccountBalance":"49197.00",
                 "ThirdPartyTransID":"","MSISDN":"254708374149","FirstName":"John","MiddleName":"","LastName":"Doe"}
                """;
        Daraja.C2bPayload payload = json.readValue(body, Daraja.C2bPayload.class);
        assertThat(payload.transId()).isEqualTo("RKTQDM7W6S");
        assertThat(payload.payerName()).isEqualTo("John Doe");
        assertThat(payload.billRefNumber()).isEqualTo("rua0042");
    }
}
