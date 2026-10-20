package ke.shulehub.payments.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.payments.MobileMoneyCallbacks;
import ke.shulehub.payments.internal.Daraja;

/**
 * Endpoints Safaricom calls. The path avoids words Daraja refuses in registered URLs (such as
 * "mpesa"); the random token in it authenticates the channel. In production also restrict these
 * paths to Safaricom's published IP ranges at the load balancer.
 */
@RestController
@RequestMapping("/api/callbacks/mobile-money/{token}")
class MobileMoneyCallbackController {

    private final MobileMoneyCallbacks callbacks;

    MobileMoneyCallbackController(MobileMoneyCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @PostMapping("/stk")
    Daraja.CallbackAck stk(@PathVariable String token, @RequestBody String body) {
        return callbacks.stkResult(token, body);
    }

    @PostMapping("/c2b/validation")
    Daraja.C2bResponse validation(@PathVariable String token, @RequestBody String body) {
        return callbacks.validateC2b(token, body);
    }

    @PostMapping("/c2b/confirmation")
    Daraja.C2bResponse confirmation(@PathVariable String token, @RequestBody String body) {
        return callbacks.confirmC2b(token, body);
    }
}
