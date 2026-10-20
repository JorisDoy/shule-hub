package ke.shulehub.shared.web;

import org.springframework.http.HttpStatus;

/** 409 Conflict: the request clashes with the record's current state. */
public class ConflictException extends ApiException {

    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, detail);
    }

    public ConflictException(String detail, Throwable cause) {
        super(HttpStatus.CONFLICT, detail, cause);
    }
}
