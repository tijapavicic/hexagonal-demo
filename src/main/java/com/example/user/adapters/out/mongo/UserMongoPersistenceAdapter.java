package com.example.user.adapters.out.mongo;

import com.example.user.application.port.out.UserPersistencePort;
import com.example.user.application.query.UserQuery;
import com.example.user.domain.model.User;
import com.example.user.domain.model.UserPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserMongoPersistenceAdapter implements UserPersistencePort {

    private final SpringDataUserRepository repository;
    private final MongoTemplate mongoTemplate;

    public UserMongoPersistenceAdapter(SpringDataUserRepository repository,
                                       MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public User save(User user) {
        return UserMongoMapper.toDomain(repository.save(UserMongoMapper.toDocument(user)));
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(UserMongoMapper::toDomain);
    }

    @Override
    public UserPage findAll(UserQuery query) {
        Query mongoQuery = buildQuery(query);
        long total = mongoTemplate.count(mongoQuery, UserDocument.class);

        mongoQuery.with(PageRequest.of(query.page(), query.size()));
        List<User> content = mongoTemplate.find(mongoQuery, UserDocument.class)
                .stream()
                .map(UserMongoMapper::toDomain)
                .toList();

        int totalPages = (int) Math.ceil((double) total / query.size());
        return new UserPage(content, total, totalPages, query.page(), query.size());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private static Query buildQuery(UserQuery query) {
        Query mongoQuery = new Query();

        if (query.name() != null && !query.name().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("name").regex(query.name(), "i"));
        }

        if (query.minAge() != null || query.maxAge() != null) {
            Criteria age = Criteria.where("age");
            if (query.minAge() != null) age = age.gte(query.minAge());
            if (query.maxAge() != null) age = age.lte(query.maxAge());
            mongoQuery.addCriteria(age);
        }

        return mongoQuery;
    }
}
