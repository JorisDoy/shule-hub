package ke.shulehub.payments;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Permission;
import ke.shulehub.payments.internal.CredentialCipher;
import ke.shulehub.payments.internal.Daraja;
import ke.shulehub.payments.internal.DarajaClient;
import ke.shulehub.payments.internal.MpesaChannel;
import ke.shulehub.payments.internal.MpesaChannelRepository;
import ke.shulehub.shared.config.ShuleHubProperties;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.NotFoundException;
import ke.shulehub.tenancy.BranchDirectory;

/** Setting up a branch's paybill or till. */
@Service
public class MpesaChannels {

    public record CallbackUrls(String stk, String validation, String confirmation) {
    }

    public record ChannelView(UUID id, UUID branchId, String shortcode, String partyB, MpesaChannelType channelType,
            MpesaEnvironment environment, boolean active, CallbackUrls callbackUrls) {
    }

    public record NewChannel(UUID branchId, String shortcode, String partyB, MpesaChannelType channelType,
            MpesaEnvironment environment, String consumerKey, String consumerSecret, String passkey) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MpesaChannelRepository channels;
    private final CredentialCipher cipher;
    private final DarajaClient daraja;
    private final BranchDirectory branches;
    private final AccessPolicy access;
    private final ShuleHubProperties properties;

    public MpesaChannels(MpesaChannelRepository channels, CredentialCipher cipher, DarajaClient daraja,
            BranchDirectory branches, AccessPolicy access, ShuleHubProperties properties) {
        this.channels = channels;
        this.cipher = cipher;
        this.daraja = daraja;
        this.branches = branches;
        this.access = access;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<ChannelView> list() {
        return channels.findAllByOrderByCreatedAtDesc().stream()
                .filter(channel -> access.has(Permission.PAYMENTS_CONFIGURE, channel.getBranchId()))
                .map(this::view)
                .toList();
    }

    /** Adds a channel and deactivates the branch's previous one, so payments go to one place. */
    @Transactional
    public ChannelView create(NewChannel request) {
        access.require(Permission.PAYMENTS_CONFIGURE, request.branchId());
        branches.require(request.branchId());
        String shortcode = request.shortcode().trim();
        if (!shortcode.matches("\\d{5,7}")) {
            throw new BadRequestException("Shortcode must be the 5 to 7 digit paybill or store number");
        }
        if (request.channelType() == MpesaChannelType.TILL && (request.partyB() == null || request.partyB().isBlank())) {
            throw new BadRequestException("For a till, give the till number customers pay to");
        }
        channels.findFirstByBranchIdAndActiveTrueOrderByCreatedAtDesc(request.branchId())
                .ifPresent(MpesaChannel::deactivate);
        byte[] token = new byte[24];
        RANDOM.nextBytes(token);
        MpesaChannel channel = new MpesaChannel(request.branchId(), shortcode,
                request.partyB() == null ? null : request.partyB().trim(), request.channelType(),
                request.environment(), cipher.encrypt(request.consumerKey().trim()),
                cipher.encrypt(request.consumerSecret().trim()), cipher.encrypt(request.passkey().trim()),
                HexFormat.of().formatHex(token));
        return view(channels.save(channel));
    }

    /** Registers our C2B validation and confirmation URLs with Safaricom for this shortcode. */
    public String registerC2bUrls(UUID channelId) {
        MpesaChannel channel = channels.findById(channelId)
                .orElseThrow(() -> new NotFoundException("M-Pesa channel not found"));
        access.require(Permission.PAYMENTS_CONFIGURE, channel.getBranchId());
        CallbackUrls urls = urls(channel);
        Daraja.RegisterUrlResponse response = daraja.registerC2bUrls(credentials(channel),
                new Daraja.RegisterUrlRequest(channel.getShortcode(), "Completed", urls.confirmation(),
                        urls.validation()));
        return response.responseDescription();
    }

    DarajaClient.Credentials credentials(MpesaChannel channel) {
        String baseUrl = channel.getEnvironment() == MpesaEnvironment.PRODUCTION
                ? properties.mpesa().productionBaseUrl()
                : properties.mpesa().sandboxBaseUrl();
        return new DarajaClient.Credentials(baseUrl, cipher.decrypt(channel.getConsumerKeyEncrypted()),
                cipher.decrypt(channel.getConsumerSecretEncrypted()));
    }

    String passkey(MpesaChannel channel) {
        return cipher.decrypt(channel.getPasskeyEncrypted());
    }

    CallbackUrls urls(MpesaChannel channel) {
        String base = properties.mpesa().callbackBaseUrl().replaceAll("/+$", "")
                + "/api/callbacks/mobile-money/" + channel.getCallbackToken();
        return new CallbackUrls(base + "/stk", base + "/c2b/validation", base + "/c2b/confirmation");
    }

    private ChannelView view(MpesaChannel channel) {
        return new ChannelView(channel.getId(), channel.getBranchId(), channel.getShortcode(), channel.getPartyB(),
                channel.getChannelType(), channel.getEnvironment(), channel.isActive(), urls(channel));
    }
}
