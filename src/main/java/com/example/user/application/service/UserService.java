package com.example.user.application.service;

import com.example.user.application.exception.UserNotFoundException;
import com.example.user.application.port.in.CreateUserUseCase;
import com.example.user.application.port.in.DeleteUserUseCase;
import com.example.user.application.port.in.GetUserUseCase;
import com.example.user.application.port.in.ListUsersUseCase;
import com.example.user.application.port.in.UpdateUserUseCase;
import com.example.user.application.port.out.UserPersistencePort;
import com.example.user.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements CreateUserUseCase, GetUserUseCase, ListUsersUseCase, UpdateUserUseCase, DeleteUserUseCase {

    private final UserPersistencePort persistencePort;

    public UserService(UserPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public User create(User user) {
        return persistencePort.save(new User(null, user.name(), user.address(), user.age()));
    }

    @Override
    public User getById(String id) {
        return persistencePort.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public List<User> getAll() {
        return persistencePort.findAll();
    }

    @Override
    public User update(String id, User user) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new UserNotFoundException(id);
        }
        return persistencePort.save(new User(id, user.name(), user.address(), user.age()));
    }

    @Override
    public void delete(String id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new UserNotFoundException(id);
        }
        persistencePort.deleteById(id);
    }
}

