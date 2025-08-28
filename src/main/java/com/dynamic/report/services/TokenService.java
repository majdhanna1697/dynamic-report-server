package com.dynamic.report.services;

import com.dynamic.report.exceptions.AuthException;
import com.dynamic.report.exceptions.EncryptionException;
import com.dynamic.report.exceptions.TokenException;
import com.dynamic.report.models.Token;
import com.dynamic.report.utils.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class TokenService {

    @Autowired
    private RSAService rsaService;

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    public String generateToken(Token token) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return generateToken(token.getId(), token.getUsername());
    }

    public String generateToken(BigInteger accountId, String username) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String decryptedToken = accountId + ":" + username;
        return rsaService.encrypt(decryptedToken);
    }

    public Token decryptToken(String token) throws EncryptionException {
        String decryptedToken = null;
        try {
            decryptedToken = rsaService.decrypt(token);
            String[] parts = decryptedToken.split(":");
            if (parts.length != 2) {
                LOGGER.error("Invalid token format: {}", token);
                throw new EncryptionException(ErrorCodes.INVALID_TOKEN, "Invalid token format", HttpStatus.BAD_REQUEST);
            }
            Token tokenModel = new Token();
            tokenModel.setId(new BigInteger(parts[0]));
            tokenModel.setUsername(parts[1]);
            return tokenModel;
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException e) {
            throw new EncryptionException(ErrorCodes.DECRYPTION_FAILED, "Invalid encrypted token", HttpStatus.BAD_REQUEST);
        }
    }

    public String extractTokenFromAuthorization(String authorization) throws TokenException {
        LOGGER.trace("Extracting token from authorization header");
        if (authorization == null || authorization.isBlank()) {
            throw new TokenException(ErrorCodes.INVALID_TOKEN, "Invalid authorization header", HttpStatus.BAD_REQUEST);
        }
        String[] parts = authorization.trim().split("\\s+");
        if (parts.length != 2 || !"Bearer".equalsIgnoreCase(parts[0]) || parts[1].isBlank()) {
            LOGGER.error("Invalid authorization header: {}", authorization);
            throw new TokenException(ErrorCodes.INVALID_TOKEN, "Invalid authorization header", HttpStatus.BAD_REQUEST);
        }
        LOGGER.debug("Authorization token extracted successfully");
        return parts[1];
    }

}
