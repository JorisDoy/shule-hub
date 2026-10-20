package ke.shulehub.shared.web;

import org.springframework.http.HttpStatus;

/** 403 Forbidden: the caller lacks the permission or scope. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String detail) {
        super(HttpStatus.FORBIDDEN, detail);
    }

    public ForbiddenException(String detail, Throwable cause) {
        super(HttpStatus.FORBIDDEN, detail, cause);
    }
}
