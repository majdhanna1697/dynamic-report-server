package com.dynamic.report.annotations.handlers;

import com.dynamic.report.exceptions.AuthException;
import com.dynamic.report.exceptions.EncryptionException;
import com.dynamic.report.exceptions.TokenException;
import com.dynamic.report.models.Token;
import com.dynamic.report.services.TokenService;
import com.dynamic.report.utils.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Aspect
@Component
public class RoleDetectingAspect {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private TokenService tokenService;

    @Before("@annotation(com.dynamic.report.annotations.RoleDetecting)")
    public void RoleDetecting(JoinPoint joinPoint) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String authorization = request.getHeader("Authorization");
        try {
            String token = tokenService.extractTokenFromAuthorization(authorization);
            Token tokenModel = tokenService.decryptToken(token);
            request.setAttribute("accountId", tokenModel.getId().toString());
            request.setAttribute("accountRole", getUserRole(tokenModel));
        } catch (TokenException | EncryptionException | AuthException e) {
            throw new IllegalArgumentException("Unauthorized");
        }
    }

    private String getUserRole(Token tokenModel) throws AuthException {
        MapSqlParameterSource queryParams = new MapSqlParameterSource()
                .addValue("accountId", tokenModel.getId())
                .addValue("username", tokenModel.getUsername());
        String sql = """
                SELECT role FROM `account` WHERE id = :accountId AND username = :username LIMIT 1
                """;
        String role = jdbcTemplate.queryForObject(sql, queryParams, String.class);
        if (Objects.isNull(role)) {
            throw new AuthException(ErrorCodes.ACCOUNT_ROLE_MISSING, "Missing account role", HttpStatus.BAD_REQUEST);
        }
        return role;
    }
}
