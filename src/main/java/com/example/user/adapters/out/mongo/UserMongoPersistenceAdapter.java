package com.example.user.adapters.out.mongo;

import com.example.user.application.port.out.UserPersistencePort;
import com.example.user.domain.model.User;
import com.example.user.domain.model.UserPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserMongoPersistenceAdapter implements UserPersistencePort {

    private final SpringDataUserRepository repository;
    private final UserMongoMapper mapper;
    private final MongoTemplate mongoTemplate;

    public UserMongoPersistenceAdapter(SpringDataUserRepository repository,
                                       UserMongoMapper mapper,
                                       MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public User save(User user) {
        return mapper.toDomain(repository.save(mapper.toDocument(user)));
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public UserPage findAll(String name, Integer minAge, Integer maxAge, int page, int size) {
        Query query = buildQuery(name, minAge, maxAge);
        long total = mongoTemplate.count(query, UserDocument.class);

        Pageable pageable = PageRequest.of(page, size);
        query.with(pageable);
        List<User> content = mongoTemplate.find(query, UserDocument.class)
                .stream().map(mapper::toDomain).toList();

        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
        return new UserPage(content, total, totalPages, page, size);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private Query buildQuery(String name, Integer minAge, Integer maxAge) {
        Query query = new Query();
        if (name != null && !name.isBlank()) {
            query.addCriteria(Criteria.where("name").regex(name, "i"));
        }
        if (minAge != null || maxAge != null) {
            Criteria ageCriteria = Criteria.where("age");
            if (minAge != null) ageCriteria = ageCriteria.gte(minAge);
            if (maxAge != null) ageCriteria = ageCriteria.lte(maxAge);
            query.addCriteria(ageCriteria);
        }
        return query;
    }
}

