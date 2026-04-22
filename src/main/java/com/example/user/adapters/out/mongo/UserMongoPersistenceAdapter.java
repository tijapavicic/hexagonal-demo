package com.example.user.adapters.out.mongo;

import com.example.user.application.port.out.UserPersistencePort;
import com.example.user.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserMongoPersistenceAdapter implements UserPersistencePort {

    private final SpringDataUserRepository repository;
    private final UserMongoMapper mapper;

    public UserMongoPersistenceAdapter(SpringDataUserRepository repository, UserMongoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserDocument saved = repository.save(mapper.toDocument(user));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}

