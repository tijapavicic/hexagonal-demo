package com.example.user.application.exception;

/**
 * @deprecated Use {@link com.example.user.domain.exception.UserNotFoundException} instead.
 *             This class is retained only for backward-compatibility and will be removed in a future version.
 */
@Deprecated(since = "1.5.0", forRemoval = true)
public class UserNotFoundException extends com.example.user.domain.exception.UserNotFoundException {
    public UserNotFoundException(String userId) {
        super(userId);
    }
}

