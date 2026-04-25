package classenrollmentsystem.common.exception;

import lombok.Getter;

@Getter
public class CustomGlobalException extends RuntimeException {

    private ErrorType errorType;

    public CustomGlobalException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

}
