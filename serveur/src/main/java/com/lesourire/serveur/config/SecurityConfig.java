package com.lesourire.serveur.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité de l'API : authentification HTTP Basic (adaptée à un client de
 * bureau sur le réseau local du cabinet), sessions désactivées.
 * Seul le point de statut est public, tout le reste exige un compte.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorisations -> autorisations
                        .requestMatchers("/api/systeme/**").permitAll()
                        .requestMatchers("/api/utilisateurs/**", "/api/prestations/**",
                                "/api/categories-prestation/**", "/api/lettres-cles/**")
                        .hasRole("ADMINISTRATEUR")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/assureurs/**", "/api/societes/**")
                        .hasRole("ADMINISTRATEUR")
                        .requestMatchers("/api/articles/**", "/api/categories-article/**",
                                "/api/fournisseurs/**", "/api/mouvements-stock/**")
                        .hasAnyRole("DENTISTE", "ASSISTANT", "ADMINISTRATEUR")
                        .requestMatchers("/api/patients/**", "/api/assureurs/**", "/api/societes/**",
                                "/api/rdv/**", "/api/praticiens/**")
                        .hasAnyRole("DENTISTE", "ASSISTANT", "SECRETAIRE", "ADMINISTRATEUR")
                        .anyRequest().authenticated())
                .httpBasic(withDefaults());
        return http.build();
    }

    /**
     * Encodeur délégué : accepte plusieurs formats ({bcrypt}, {noop}...),
     * ce qui permet de migrer le compte initial vers bcrypt sans casse.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
