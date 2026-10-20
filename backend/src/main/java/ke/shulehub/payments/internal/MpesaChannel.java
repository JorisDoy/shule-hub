package ke.shulehub.payments.internal;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.payments.MpesaChannelType;
import ke.shulehub.payments.MpesaEnvironment;
import ke.shulehub.shared.persistence.TenantScopedEntity;

/**
 * A branch's M-Pesa paybill or till with its Daraja credentials (encrypted). Money settles
 * straight into the school's own shortcode; Shule Hub never holds funds.
 */
@Entity
@Table(name = "mpesa_channel")
public class MpesaChannel extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "shortcode", nullable = false)
    private String shortcode;

    /** For tills: the till number customers pay to, when it differs from the store shortcode. */
    @Column(name = "party_b")
    private String partyB;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private MpesaChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false)
    private MpesaEnvironment environment;

    @Column(name = "consumer_key_enc", nullable = false)
    private String consumerKeyEncrypted;

    @Column(name = "consumer_secret_enc", nullable = false)
    private String consumerSecretEncrypted;

    @Column(name = "passkey_enc", nullable = false)
    private String passkeyEncrypted;

    /** Random secret in the callback URLs; identifies the channel when Safaricom calls back. */
    @Column(name = "callback_token", nullable = false, unique = true, updatable = false)
    private String callbackToken;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected MpesaChannel() {
    }

    public MpesaChannel(UUID branchId, String shortcode, String partyB, MpesaChannelType channelType,
            MpesaEnvironment environment, String consumerKeyEncrypted, String consumerSecretEncrypted,
            String passkeyEncrypted, String callbackToken) {
        this.branchId = branchId;
        this.shortcode = shortcode;
        this.partyB = partyB;
        this.channelType = channelType;
        this.environment = environment;
        this.consumerKeyEncrypted = consumerKeyEncrypted;
        this.consumerSecretEncrypted = consumerSecretEncrypted;
        this.passkeyEncrypted = passkeyEncrypted;
        this.callbackToken = callbackToken;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getShortcode() {
        return shortcode;
    }

    /** The party that receives the money: the till number for tills, otherwise the shortcode. */
    public String receivingParty() {
        return partyB == null || partyB.isBlank() ? shortcode : partyB;
    }

    public String getPartyB() {
        return partyB;
    }

    public MpesaChannelType getChannelType() {
        return channelType;
    }

    public MpesaEnvironment getEnvironment() {
        return environment;
    }

    public String getConsumerKeyEncrypted() {
        return consumerKeyEncrypted;
    }

    public String getConsumerSecretEncrypted() {
        return consumerSecretEncrypted;
    }

    public String getPasskeyEncrypted() {
        return passkeyEncrypted;
    }

    public String getCallbackToken() {
        return callbackToken;
    }

    public boolean isActive() {
        return active;
    }
}
