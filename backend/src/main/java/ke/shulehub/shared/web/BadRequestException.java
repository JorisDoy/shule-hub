package ke.shulehub.shared.web;

import org.springframework.http.HttpStatus;

/** 400 Bad Request: input the server cannot accept. */
public class BadRequestException extends ApiException {

    public BadRequestException(String detail) {
        super(HttpStatus.BAD_REQUEST, detail);
    }

    public BadRequestException(String detail, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, detail, cause);
    }
}
