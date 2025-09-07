// Fichier : src/main/java/com/mediatower/backend/exception/InvalidPromotionException.java

package com.mediatower.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidPromotionException extends RuntimeException {

    public InvalidPromotionException(String message) {
        super(message);
    }
}