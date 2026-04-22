package com.example.user.application.service;

import com.example.user.application.exception.UserNotFoundException;
import com.example.user.application.port.out.UserPersistencePort;
import com.example.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserPersistencePort persistencePort;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(persistencePort);
    }

    @Test
    void createShouldPersistUserWithoutId() {
        User input = new User("client-id", "Alice", "Berlin", 30);
        User saved = new User("generated-id", "Alice", "Berlin", 30);
        when(persistencePort.save(any(User.class))).thenReturn(saved);

        User result = userService.create(input);

        assertEquals("generated-id", result.id());
        verify(persistencePort).save(new User(null, "Alice", "Berlin", 30));
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(persistencePort.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById("missing"));
    }
}

