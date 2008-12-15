package org.limewire.promotion.exceptions;

public class PromotionException extends Exception {

    public PromotionException() {
    }

    public PromotionException(String message) {
        super(message);
    }

    public PromotionException(Throwable cause) {
        super(cause);
    }

    public PromotionException(String message, Throwable cause) {
        super(message, cause);
    }

}
