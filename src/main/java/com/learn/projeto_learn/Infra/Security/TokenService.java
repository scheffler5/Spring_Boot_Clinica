package com.learn.projeto_learn.Infra.Security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.learn.projeto_learn.Domain.User.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token = JWT.create()
                    .withIssuer("auth-api") // Quem gerou (nome da sua aplicação)
                    .withSubject(usuario.getLogin()) // Quem é o dono do token (salvamos o Login)
                    .withExpiresAt(genExpirationDate()) // Quando expira
                    .sign(algorithm); // Assinatura digital
            return token;
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    // --- VALIDAR TOKEN (Para as próximas requisições) ---
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token) // Se o token for inválido ou expirado, lança exceção aqui
                    .getSubject(); // Retorna o Login que estava escondido no token
        } catch (JWTVerificationException exception) {
            return ""; // Token inválido
        }
    }

    // --- Tempo de Expiração (2 horas) ---
    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
