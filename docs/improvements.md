# Key Findings
# 1.5.1
- High: the app ships with a fallback JWT signing secret and fallback login credentials in application.yml (line 32). If someone deploys without overriding APP_JWT_SECRET, APP_USERNAME, and APP_PASSWORD_HASH, they can mint valid tokens with known defaults.
- High: the unauthenticated security test does not actually verify security. @WithMockUser is applied at class level in UserControllerIntegrationTest.java (line 35), and the test itself accepts either 200 or 401 in the same file (line 151). That means a broken auth rule could still pass CI.
- Medium: role extraction in SecurityConfig.java (line 123) is very likely wrong for the documented behavior. The code sets one claim name, "realm_access.roles", but the comments say it supports both nested Keycloak roles and flat custom-token roles. As written, those two formats are not actually handled as a real fallback strategy.
- Medium: the name filter in UserMongoPersistenceAdapter.java (line 66) treats user input as raw regex. That means name=.* matches everything, and regex metacharacters can change behavior in ways the API contract does not promise.
- Medium: pagination in UserMongoPersistenceAdapter.java (line 48) has no explicit sort, so page contents are not stable. Inserts/updates can reshuffle what clients see between requests.
- Medium: the repo currently does not build cleanly in this environment. mvn test failed with release version 21 not supported because pom.xml (line 20) targets Java 21 while README.md (line 4) still advertises Java 17.

- Architecture Read
The overall structure is solid. The domain model self-validates in User.java (line 7), 
- the application layer stays thin in UserService.java (line 15), 
- inbound and outbound adapters are separated cleanly, 
- and the ArchUnit rules in ArchitectureTest.java (line 1) 
- do a nice job protecting the hexagonal boundaries.

The tradeoff is that the service layer is mostly CRUD orchestration right now, 
so the architecture is stronger than the domain complexity currently needs. 
That is not bad, but it means the security and operational details are where most of the real risk lives.