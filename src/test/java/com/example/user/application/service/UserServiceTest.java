package com.example.user.application.service;

import com.example.user.domain.exception.UserNotFoundException;
import com.example.user.application.port.out.UserPersistencePort;
import com.example.user.application.query.UserQuery;
import com.example.user.domain.model.User;
import com.example.user.domain.model.UserPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    void createShouldStripClientSuppliedId() {
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

    @Test
    void getAllShouldDelegateToPersistenceWithQuery() {
        UserQuery query = UserQuery.of(null, null, null, 0, 20);
        UserPage page = new UserPage(List.of(), 0, 0, 0, 20);
        when(persistencePort.findAll(query)).thenReturn(page);

        UserPage result = userService.getAll(query);

        assertEquals(page, result);
        verify(persistencePort).findAll(query);
    }

    @Test
    void updateShouldThrowWhenUserNotFound() {
        when(persistencePort.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.update("missing", new User(null, "X", "Y", 1)));

        verify(persistencePort, never()).save(any());
    }

    @Test
    void deleteShouldThrowWhenUserNotFound() {
        when(persistencePort.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.delete("missing"));

        verify(persistencePort, never()).deleteById(any());
    }

    @Test
    void userQueryShouldRejectNegativePage() {
        assertThrows(IllegalArgumentException.class,
                () -> UserQuery.of(null, null, null, -1, 20));
    }

    @Test
    void userQueryShouldRejectZeroSize() {
        assertThrows(IllegalArgumentException.class,
                () -> UserQuery.of(null, null, null, 0, 0));
    }
}
