package com.example.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @Test
    void shouldAllowBoundaryAges() {
        User newborn = assertDoesNotThrow(() -> new User(null, "Alice", "Berlin", 0));
        User oldestAllowed = assertDoesNotThrow(() -> new User(null, "Bob", "Hamburg", 150));

        assertEquals(0, newborn.age());
        assertEquals(150, oldestAllowed.age());
    }

    @Test
    void shouldAllowNullAge() {
        User user = assertDoesNotThrow(() -> new User(null, "Alice", "Berlin", null));

        assertEquals(null, user.age());
    }

    @Test
    void shouldRejectBlankOrNullName() {
        IllegalArgumentException nullName = assertThrows(IllegalArgumentException.class,
                () -> new User(null, null, "Berlin", 20));
        IllegalArgumentException blankName = assertThrows(IllegalArgumentException.class,
                () -> new User(null, "   ", "Berlin", 20));

        assertEquals("name must not be blank", nullName.getMessage());
        assertEquals("name must not be blank", blankName.getMessage());
    }

    @Test
    void shouldRejectBlankOrNullAddress() {
        IllegalArgumentException nullAddress = assertThrows(IllegalArgumentException.class,
                () -> new User(null, "Alice", null, 20));
        IllegalArgumentException blankAddress = assertThrows(IllegalArgumentException.class,
                () -> new User(null, "Alice", "   ", 20));

        assertEquals("address must not be blank", nullAddress.getMessage());
        assertEquals("address must not be blank", blankAddress.getMessage());
    }

    @Test
    void shouldRejectAgeOutsideAllowedRange() {
        IllegalArgumentException negativeAge = assertThrows(IllegalArgumentException.class,
                () -> new User(null, "Alice", "Berlin", -1));
        IllegalArgumentException tooHighAge = assertThrows(IllegalArgumentException.class,
                () -> new User(null, "Alice", "Berlin", 151));

        assertEquals("age must be between 0 and 150", negativeAge.getMessage());
        assertEquals("age must be between 0 and 150", tooHighAge.getMessage());
    }
}
