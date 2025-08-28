package com.dynamic.report.controllers;

import com.asia.model.LoginRequest;
import com.asia.model.LoginResponse;
import com.dynamic.report.exceptions.AuthException;
import com.dynamic.report.exceptions.EncryptionException;
import com.dynamic.report.exceptions.TokenException;
import com.dynamic.report.models.LoggedAccount;
import com.dynamic.report.models.Token;
import com.dynamic.report.services.RSAService;
import com.dynamic.report.services.TokenService;
import com.dynamic.report.utils.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Controller
public class DynamicReportAuthController {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private RSAService rsaService;

    @Autowired
    private TokenService tokenService;

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicReportAuthController.class);

    public LoginResponse login(String authorization, LoginRequest loginRequest) throws AuthException, EncryptionException, TokenException {
        LOGGER.trace("Starting login process");
        if (authorization != null && !authorization.isBlank()) {
            LOGGER.debug("Authorization header found: {}", authorization);
            String token = tokenService.extractTokenFromAuthorization(authorization);
            Token tokenModel = loginByToken(token);
            LOGGER.info("Login successful using token for user '{}'", tokenModel.getUsername());
            return mapAuthResponse(token, tokenModel.getUsername());
        } else {
            LOGGER.debug("No authorization header found, proceeding with username/password login for '{}'", loginRequest.getUsername());
            validateLoginRequest(loginRequest);
            String token = loginByUsernameAndPassword(loginRequest.getUsername(), loginRequest.getPassword());
            LOGGER.info("Login successful using username/password for '{}'", loginRequest.getUsername());
            return mapAuthResponse(token, loginRequest.getUsername());
        }
    }

    private void validateLoginRequest(LoginRequest loginRequest) throws AuthException {
        LOGGER.trace("Validating login request");
        if (loginRequest == null) {
            LOGGER.error("Login request object is null");
            throw new AuthException(ErrorCodes.INVALID_REQUEST, "Login request cannot be null", HttpStatus.BAD_REQUEST);
        }
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank()) {
            LOGGER.warn("Invalid login request: missing username");
            throw new AuthException(ErrorCodes.INVALID_USERNAME, "Username is required", HttpStatus.BAD_REQUEST);
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
            LOGGER.warn("Invalid login request: missing password for username={}", loginRequest.getUsername());
            throw new AuthException(ErrorCodes.INVALID_PASSWORD, "Password is required", HttpStatus.BAD_REQUEST);
        }
        LOGGER.debug("Login request validated successfully for username={}", loginRequest.getUsername());
    }

    private Token loginByToken(String token) throws EncryptionException, AuthException {
        LOGGER.trace("Validating login by token");
        Token tokenModel = tokenService.decryptToken(token);
        LOGGER.debug("Token decrypted: accountId={}, username={}", tokenModel.getId(), tokenModel.getUsername());
        validateAccountExist(tokenModel);
        LOGGER.info("Token validated successfully for user '{}'", tokenModel.getUsername());
        return tokenModel;
    }

    private void validateAccountExist(Token token) throws AuthException {
        LOGGER.trace("Checking if account exists for accountId={} and username={}", token.getId(), token.getUsername());
        String sqlQuery = """
                SELECT id FROM `account` WHERE id = :accountId AND username = :username LIMIT 1
                """;
        MapSqlParameterSource queryParams = new MapSqlParameterSource()
                .addValue("accountId", token.getId())
                .addValue("username", token.getUsername());
        List<LoggedAccount> accounts = jdbcTemplate.query(sqlQuery, queryParams, new BeanPropertyRowMapper<>(LoggedAccount.class));
        if (accounts.isEmpty()) {
            LOGGER.warn("Account not found for accountId={} and username={}", token.getId(), token.getUsername());
            throw new AuthException(ErrorCodes.ACCOUNT_NOT_FOUND, "Account not found", HttpStatus.BAD_REQUEST);
        }
        LOGGER.debug("Account validation successful for accountId={} and username={}", token.getId(), token.getUsername());
    }

    private String loginByUsernameAndPassword(String username, String password) throws AuthException, EncryptionException {
        LOGGER.trace("Login by username({}) and password", username);
        String sqlQuery = """
                SELECT id, password FROM `account` WHERE username = :username LIMIT 1
                """;
        MapSqlParameterSource queryParams = new MapSqlParameterSource()
                .addValue("username", username.toLowerCase());
        List<LoggedAccount> accounts = jdbcTemplate.query(sqlQuery, queryParams, new BeanPropertyRowMapper<>(LoggedAccount.class));
        if (accounts.isEmpty()) {
            LOGGER.warn("No account found for username={}", username);
            throw new AuthException(ErrorCodes.ACCOUNT_NOT_FOUND, "Account not found", HttpStatus.BAD_REQUEST);
        }
        String encryptedPassword = accounts.get(0).getPassword();
        try {
            LOGGER.debug("Decrypting password for username={}", username);
            String decryptedPassword = rsaService.decrypt(encryptedPassword);
            if (!password.equals(decryptedPassword)) {
                LOGGER.error("Invalid password attempt for username={}", username);
                throw new AuthException(ErrorCodes.INVALID_LOGIN_PASSWORD, "Invalid password", HttpStatus.BAD_REQUEST);
            }
            BigInteger accountId = accounts.get(0).getId();
            if (accountId == null || accountId.toString().isBlank()) {
                LOGGER.error("Invalid account ID retrieved for username={}", username);
                throw new AuthException(ErrorCodes.INVALID_ACCOUNT_ID, "Invalid account ID", HttpStatus.BAD_REQUEST);
            }
            LOGGER.info("Login successful for username={} with accountId={}", username, accountId);
            return tokenService.generateToken(accountId, username);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException e) {
            LOGGER.error("Password decryption failed for username={}", username, e);
            throw new EncryptionException(ErrorCodes.DECRYPTION_FAILED, "Invalid encrypted password", HttpStatus.BAD_REQUEST);
        }
    }

    private LoginResponse mapAuthResponse(String token, String username) {
        LOGGER.trace("Mapping login response for username={}", username);
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken(token);
        loginResponse.setUsername(username);
        LOGGER.debug("LoginResponse created for username={}", username);
        return loginResponse;
    }

}
