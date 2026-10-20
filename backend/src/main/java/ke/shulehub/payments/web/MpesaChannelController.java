package ke.shulehub.payments.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.payments.MpesaChannels;
import ke.shulehub.payments.MpesaChannels.ChannelView;
import ke.shulehub.payments.MpesaChannelType;
import ke.shulehub.payments.MpesaEnvironment;
import ke.shulehub.shared.tenancy.TenantContext;

@RestController
@RequestMapping("/api/payments/mpesa/channels")
class MpesaChannelController {

    record CreateChannel(@NotNull UUID branchId, @NotBlank String shortcode, String partyB,
            @NotNull MpesaChannelType channelType, @NotNull MpesaEnvironment environment, @NotBlank String consumerKey,
            @NotBlank String consumerSecret, @NotBlank String passkey) {
    }

    private final MpesaChannels channels;

    MpesaChannelController(MpesaChannels channels) {
        this.channels = channels;
    }

    @GetMapping
    List<ChannelView> list() {
        TenantContext.require();
        return channels.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ChannelView create(@Valid @RequestBody CreateChannel request) {
        TenantContext.require();
        return channels.create(new MpesaChannels.NewChannel(request.branchId(), request.shortcode(), request.partyB(),
                request.channelType(), request.environment(), request.consumerKey(), request.consumerSecret(),
                request.passkey()));
    }

    @PostMapping("/{id}/register-c2b")
    Map<String, String> registerC2b(@PathVariable UUID id) {
        TenantContext.require();
        return Map.of("responseDescription", String.valueOf(channels.registerC2bUrls(id)));
    }
}
