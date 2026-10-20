package ke.shulehub.shared.web;

import org.springframework.http.HttpStatus;

/** 502 Bad Gateway: an upstream service such as M-Pesa failed. */
public class GatewayException extends ApiException {

    public GatewayException(String detail) {
        super(HttpStatus.BAD_GATEWAY, detail);
    }

    public GatewayException(String detail, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, detail, cause);
    }
}
