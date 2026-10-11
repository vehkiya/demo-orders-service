package app.kerrlab.demo.orders;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * RFC 9457 problem responses. Spring MVC renders any {@link ErrorResponseException} as {@code application/problem+json}.
 */
public final class Problems {

    private Problems() {
    }

    public static ErrorResponseException of(HttpStatus status, String type, String title, String detail) {
        return of(status, type, title, detail, null);
    }

    public static ErrorResponseException of(HttpStatus status, String type, String title, String detail,
            Throwable cause) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("/problems/" + type));
        problem.setTitle(title);
        return new ErrorResponseException(status, problem, cause);
    }
}
