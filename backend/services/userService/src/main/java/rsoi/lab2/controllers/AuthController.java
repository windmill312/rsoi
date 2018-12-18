package rsoi.lab2.controllers;

import org.hibernate.mapping.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import rsoi.lab2.entity.Role;
import rsoi.lab2.entity.Token;
import rsoi.lab2.entity.User;
import rsoi.lab2.exception.AppException;
import rsoi.lab2.model.RoleName;
import rsoi.lab2.model.TokenType;
import rsoi.lab2.payload.ApiResponse;
import rsoi.lab2.payload.JwtAuthenticationResponse;
import rsoi.lab2.payload.LoginRequest;
import rsoi.lab2.payload.SignUpUserRequest;
import rsoi.lab2.repositories.RoleRepository;
import rsoi.lab2.repositories.UserRepository;
import rsoi.lab2.security.JwtTokenProvider;

import javax.validation.Valid;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Value("${app.jwtAccessExpirationInMs}")
    private int jwtAccessExpirationInMs;

    @Value("${app.ourServiceUuid}")
    private String ourServiceUuid;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider tokenProvider;

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String headerAuth, @RequestBody LoginRequest loginRequest) {

        logger.info("Get refresh-token request from: " + loginRequest.getServiceUuid() + "\n");

        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail()).orElseThrow(() ->
                new UsernameNotFoundException("User not found with username or email: " + loginRequest.getUsernameOrEmail())
        );

        HashSet<Token> tokenSet = (HashSet<Token>) user.getTokens();
        boolean contains = false;
        Token checkToken = new Token();
        checkToken.setValue(headerAuth);

        for (Token token: tokenSet) {
            if (token.getValue().equals(checkToken.getValue())) {
                contains = true;
            }
        }

        if (contains) {
            String jwtAccess = tokenProvider.generateToken(user.getEmail());
            String jwtRefresh = tokenProvider.generateToken(user.getEmail());

            for (Token token: tokenSet) {
                if (token.getServiceUuid().equals(loginRequest.getServiceUuid())) {
                    if (token.getTokenType().equals(TokenType.ACCESS_TOKEN)) {
                        token.setValue(jwtAccess);
                    }
                    else
                        token.setValue(jwtRefresh);
                }
            }

            userRepository.save(user);

            return ResponseEntity.ok(new JwtAuthenticationResponse(jwtAccess, jwtRefresh, jwtAccessExpirationInMs));
        }
        else
        {
            throw new UsernameNotFoundException("Token is invalid");
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        logger.info("Get auth request from: " + loginRequest.getServiceUuid() + "\n");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwtAccess = tokenProvider.generateToken(loginRequest.getUsernameOrEmail());
        String jwtRefresh = tokenProvider.generateToken(loginRequest.getUsernameOrEmail());

        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail())
                .orElseThrow(() ->
                new UsernameNotFoundException("User not found with username or email : " + loginRequest.getUsernameOrEmail())
        );

        //todo опасно
        HashSet<Token> tokenSet = user.getTokens();

        Token accessToken = new Token();
        accessToken.setTokenType(TokenType.ACCESS_TOKEN);
        accessToken.setValue(jwtAccess);
        accessToken.setServiceUuid(loginRequest.getServiceUuid());
        tokenSet.add(accessToken);

        Token refreshToken = new Token();
        accessToken.setTokenType(TokenType.REFRESH_TOKEN);
        accessToken.setValue(jwtRefresh);
        accessToken.setServiceUuid(loginRequest.getServiceUuid());
        tokenSet.add(refreshToken);

        user.setTokens(tokenSet);
        userRepository.save(user);

        return ResponseEntity.ok(new JwtAuthenticationResponse(jwtAccess, jwtRefresh, jwtAccessExpirationInMs));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpUserRequest signUpUserRequest) {
        if(userRepository.existsByUsername(signUpUserRequest.getUsername())) {
            return new ResponseEntity<>(new ApiResponse(false, "Username is already taken!"),
                    HttpStatus.BAD_REQUEST);
        }

        if(userRepository.existsByEmail(signUpUserRequest.getEmail())) {
            return new ResponseEntity<>(new ApiResponse(false, "Email Address already in use!"),
                    HttpStatus.BAD_REQUEST);
        }

        User user = new User(signUpUserRequest.getName(), signUpUserRequest.getUsername(),
                signUpUserRequest.getEmail(), signUpUserRequest.getPassword());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new AppException("User Role not set."));

        user.setRoles(Collections.singleton(userRole));

        User result = userRepository.save(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(result.getUsername()).toUri();

        return ResponseEntity.created(location).body(new ApiResponse(true, "User registered successfully"));
    }
}