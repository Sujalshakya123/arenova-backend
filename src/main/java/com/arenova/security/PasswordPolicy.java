package com.arenova.security;

import org.apache.coyote.BadRequestException;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final String MESSAGE =
            "Password must be 8-16 characters and include an uppercase letter, a number, and a symbol.";

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$"
    );

    private PasswordPolicy() {
    }

    public static void requireStrongPassword(String password) throws BadRequestException {
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new BadRequestException(MESSAGE);
        }
    }
}
