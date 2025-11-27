package com.learn.projeto_learn.Infra.Security;

import com.learn.projeto_learn.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    TokenService tokenService;

    @Autowired
    UsuarioRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. Recupera o token do cabeçalho
        var token = this.recoverToken(request);

        // 2. Se o token existir, valida
        if (token != null) {
            var login = tokenService.validateToken(token); // Retorna o email/login do usuário

            if (login != null && !login.isEmpty()) {
                // 3. Busca o usuário no banco
                UserDetails user = userRepository.findByLogin(login);

                if (user != null) {
                    // 4. Cria o objeto de autenticação do Spring
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                    // 5. Salva no contexto (Diz pro Spring: "Logado com sucesso")
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // 6. Continua para o próximo filtro (ou para o Controller)
        filterChain.doFilter(request, response);
    }

    // Método auxiliar para pegar o token "limpo" (sem o prefixo 'Bearer ')
    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}