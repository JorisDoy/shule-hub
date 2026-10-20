package ke.shulehub.shared.web;

import org.springframework.http.HttpStatus;

/** 404 Not Found. Also used for records that belong to another tenant. */
public class NotFoundException extends ApiException {

    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, detail);
    }

    public NotFoundException(String detail, Throwable cause) {
        super(HttpStatus.NOT_FOUND, detail, cause);
    }
}
