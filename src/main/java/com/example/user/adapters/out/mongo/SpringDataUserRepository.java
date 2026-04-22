package com.example.user.adapters.out.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataUserRepository extends MongoRepository<UserDocument, String> {
}

