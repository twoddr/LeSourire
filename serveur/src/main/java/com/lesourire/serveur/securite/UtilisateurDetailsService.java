package com.lesourire.serveur.securite;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.lesourire.serveur.entite.Utilisateur;
import com.lesourire.serveur.repository.UtilisateurRepository;

import java.util.List;

/** Charge les comptes depuis la table utilisateur pour Spring Security. */
@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String nomUtilisateur) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository
                .findByNomUtilisateurAndActifTrue(nomUtilisateur)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur inconnu ou désactivé : " + nomUtilisateur));

        return new User(
                utilisateur.getNomUtilisateur(),
                utilisateur.getMotDePasse(),
                List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name())));
    }
}
