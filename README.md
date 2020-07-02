# Kalah Game

## Build, Test and Run
To build application and run all the tests:
```
./build.sh
```

To start application locally on port `8080`:
```
./run.sh
```

## Project structure

### REST API
REST Layer is using Spring Web library. Jackson library used to serialise/deserialize objects.
`Game Controller` - rest controller which handles game api requests to create a game, make a move, list games and get game status. 
`Authentication Controller` - rest controller to login/signup with the username/password credentials.

### DTO
Immutable POJOs marked with `@Value` annotation from lombok, which are used to pass information between client and server.

### Service layer
`GameService` contains the game logic, game initialization, validating and making moves and finishing up the game.
In case of invalid move `IllegalMoveException` is thrown with a detailed message.
If game not found in the database, the `GameNotFoundException` is thrown.

`MyUserDetailsService` is a custom implementation of `UserDetailsService` from Spring Security. 
It has methods to add a new user and to load a user by its name.
If username not found `GameNotFoundException` is thrown.

### Exception handling
A global exception handler is `KalahApplicationExceptionHandler`. It handles business logic exceptions and returns
`ErrorResponseDto` with http status and appropriate error message.

### Security
`SecurityConfig` configure security by extending `WebSecurityConfigurerAdapter`, define endpoints that need authentication,
configure filters, csrf and exception handling.  
Json web token library [jjwt](https://github.com/jwtk/jjwt) is used for generating and validation 
the jwt token, which should be passed in every request in a format `"Authorization", "Bearer jwt_token"`
`JwtTokenProvider` - jwt token creation, parsing and validation.
`JwtTokenAuthenticationFilter` - filtering non-authenticated requests.
`JwtAuthenticationEntryPoint` - handling non authenticated requests by sending back `ErrorResponseDto` with `UNAUTHORIZED` http status. 

### Database
Everything is stored in H2 in-memory database. It was selected to speed up the dev process, can be easily replaced with
other SQL database (e.g. Postgres, MySQL, Oracle) by changing the spring properties in `application.yaml`. 
H2 Console web page can be accessed [here](http://localhost:8080/h2-console), username `sa` password ``.

### Model
`Game` - entity for storing the game, with list of pits.
`Pit` - entity to store one pit with its weight (amount of stones), a part (`SOUTH` or `NORTH`), and type of pit (`isKalah` property).
`User` - implementation of `UserDetails` from spring security to store users in the database.   

### DAO layer
Spring Data JPA library manages the dao layer. For the simple CRUD operation is it not needed to write any SQL, 
it will be generated. In case it is necessary to write some custom SQL code, it is still possible, by adding the class 
marked as `@Component`with same name as Repository (e.g. `GameRepositoryImpl`) where you inject `EntityManager` and can use 
is to write any custom queries. However, for small queries it is also possible to use `@Query` annotation on the method 
defined in a repository interface itself.   
`GameRepository` used db operations for `Game` entity.
`UserRepository` used for user-related operations, with one custom method - `findByUsername`.

### Unit and Integration tests
`KalahApplicationTests` - integration test which set up the full spring context on a random port by using `@SpringBootTest` annotation.
It is  testing the full cycle, authentication, game creation, making moves, listing games and getting game status.
`GameControllerTest` unit tests for `GameController` using `MockMvc` to perform requests.
`GameServiceTest` unit testing of the game business logic with covering 100% of `GameService` methods and lines of code.    

### Swagger 
Swagger and Swagger UI is also integrated and configured in `SwaggerConfig` configuration, so api can be browsed by [swagger ui webpage](http://localhost:8080/swagger-ui.html) or as a [json](http://localhost:8080/v2/api-docs)
 
### Postman
I also used Postman to test the api endpoints, you can find postman collection and environment in the project root.

### Future improvements
- Now every logged in user can play any game for part, an improvement will be to connect game with users, 
so that only two different users can play same the game every for its own part.

- Status response after making a move does not give any information about the current turn, I didn't want to change the response body, 
but this might be useful enhancement in next future.  

- Code coverage is still not totally 100%, this is definitely a point to improve, though main logic in service layer is 100% methods/lines of code covered.

- Javadoc is missing, i personally like to avoid too much javadoc because the code should be self-documented and maintaining 
javadoc requires additional time and effort. However, in production code it is required for some parts of the code
which can be used for extensions, so I point it as a possible improvement.   

### Endpoints
1. Create a game:

```
curl --header "Content-Type: application/json" \ --request POST \ http://<host>:<port>/games
```

Response:

```
HTTP code: 201
Response Body: { "id": "1234", "uri": "http://<host>:<port>/games/1234" }
id: unique identifier of a game url: link to the game created
```

2. Make a move:

```
curl --header "Content-Type: application/json" \ --request PUT \ http://<host>:<port>/games/{gameId}/pits/{pitId}
```

Response:

```
HTTP code: 200
​
Response Body: {"id":"1234","url":"http://<host>:<port>/games/1234","status":{"1":"4","2":"4","3":"4","4":"4","5":"4","6":"4","7":"0","8":"4"," 9":"4","10":"4","11":"4","12":"4","13":"4","14":"0"}}
status: json object key-value, where key is the pitId and value is the number of stones in the pit
```