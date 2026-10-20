package ke.shulehub.shared.web;

import org.springframework.http.HttpStatus;

/** An error with an HTTP status and a message safe to show to the user. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
    }

    public ApiException(HttpStatus status, String detail, Throwable cause) {
        super(detail, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
