package com.lesourire.serveur.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.TypeMouvementStock;
import com.lesourire.commun.dto.ArticleDTO;
import com.lesourire.commun.dto.CategorieArticleDTO;
import com.lesourire.commun.dto.FournisseurDTO;
import com.lesourire.commun.dto.MouvementStockDTO;
import com.lesourire.serveur.entite.Article;
import com.lesourire.serveur.entite.CategorieArticle;
import com.lesourire.serveur.entite.Fournisseur;
import com.lesourire.serveur.entite.MouvementStock;
import com.lesourire.serveur.repository.ArticleRepository;
import com.lesourire.serveur.repository.CategorieArticleRepository;
import com.lesourire.serveur.repository.FournisseurRepository;
import com.lesourire.serveur.repository.MouvementStockRepository;

@Service
@Transactional
public class StockService {

    private final ArticleRepository articleRepository;
    private final CategorieArticleRepository categorieRepository;
    private final FournisseurRepository fournisseurRepository;
    private final MouvementStockRepository mouvementRepository;
    private final AuditService auditService;

    public StockService(ArticleRepository articleRepository,
            CategorieArticleRepository categorieRepository,
            FournisseurRepository fournisseurRepository,
            MouvementStockRepository mouvementRepository,
            AuditService auditService) {
        this.articleRepository = articleRepository;
        this.categorieRepository = categorieRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.mouvementRepository = mouvementRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------ catégories

    @Transactional(readOnly = true)
    public List<CategorieArticleDTO> categories() {
        return categorieRepository.findAllByOrderByLibelleAsc().stream()
                .map(CategorieArticle::versDTO)
                .toList();
    }

    // --------------------------------------------------------------- articles

    @Transactional(readOnly = true)
    public List<ArticleDTO> rechercherArticles(String recherche, boolean inclureInactifs) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return articleRepository.rechercher(q, inclureInactifs).stream()
                .map(Article::versDTO)
                .toList();
    }

    public ArticleDTO creerArticle(ArticleDTO dto, String auteur) {
        validerArticle(dto);
        Article a = new Article();
        appliquerArticle(a, dto);
        // Le stock se met à jour uniquement via les mouvements (trigger)
        a.setQuantiteStock(BigDecimal.ZERO);
        a = articleRepository.save(a);
        auditService.enregistrer(auteur, "CREATION", "article", a.getId(),
                "Création de l'article " + a.getNom());
        return a.versDTO();
    }

    public ArticleDTO modifierArticle(Long id, ArticleDTO dto, String auteur) {
        validerArticle(dto);
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Article introuvable."));
        appliquerArticle(a, dto);
        a = articleRepository.save(a);
        auditService.enregistrer(auteur, "MODIFICATION", "article", a.getId(),
                "Modification de l'article " + a.getNom());
        return a.versDTO();
    }

    // ----------------------------------------------------------- fournisseurs

    @Transactional(readOnly = true)
    public List<FournisseurDTO> listerFournisseurs(boolean inclureInactifs) {
        List<Fournisseur> liste = inclureInactifs
                ? fournisseurRepository.findAllByOrderByNomAsc()
                : fournisseurRepository.findByActifTrueOrderByNomAsc();
        return liste.stream().map(Fournisseur::versDTO).toList();
    }

    public FournisseurDTO creerFournisseur(FournisseurDTO dto, String auteur) {
        if (dto.nom == null || dto.nom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        if (fournisseurRepository.existsByNomIgnoreCase(dto.nom.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un fournisseur porte déjà ce nom.");
        }
        Fournisseur f = new Fournisseur();
        appliquerFournisseur(f, dto);
        f = fournisseurRepository.save(f);
        auditService.enregistrer(auteur, "CREATION", "fournisseur", f.getId(),
                "Création du fournisseur " + f.getNom());
        return f.versDTO();
    }

    public FournisseurDTO modifierFournisseur(Long id, FournisseurDTO dto, String auteur) {
        if (dto.nom == null || dto.nom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        Fournisseur f = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Fournisseur introuvable."));
        if (fournisseurRepository.existsByNomIgnoreCaseAndIdNot(dto.nom.trim(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un fournisseur porte déjà ce nom.");
        }
        appliquerFournisseur(f, dto);
        f = fournisseurRepository.save(f);
        auditService.enregistrer(auteur, "MODIFICATION", "fournisseur", f.getId(),
                "Modification du fournisseur " + f.getNom());
        return f.versDTO();
    }

    // ------------------------------------------------------------- mouvements

    @Transactional(readOnly = true)
    public List<MouvementStockDTO> mouvementsArticle(Long articleId) {
        return mouvementRepository.findByArticleIdOrderByDateMouvementDesc(articleId).stream()
                .map(MouvementStock::versDTO)
                .toList();
    }

    public MouvementStockDTO enregistrerMouvement(MouvementStockDTO dto, String auteur) {
        if (dto.articleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article obligatoire.");
        }
        if (dto.type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de mouvement obligatoire.");
        }
        if (dto.quantite == null || dto.quantite.compareTo(BigDecimal.ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La quantité ne peut pas être nulle.");
        }
        // ENTREE / SORTIE / PEREMPTION : quantité positive ; AJUSTEMENT : signé
        if (dto.type != TypeMouvementStock.AJUSTEMENT
                && dto.quantite.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La quantité doit être strictement positive.");
        }

        Article article = articleRepository.findById(dto.articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Article introuvable."));

        MouvementStock m = new MouvementStock();
        m.setArticle(article);
        m.setType(dto.type);
        m.setQuantite(dto.type == TypeMouvementStock.AJUSTEMENT
                ? dto.quantite
                : dto.quantite.abs());
        m.setPrixUnitaire(dto.prixUnitaire);
        m.setDateMouvement(dto.dateMouvement == null ? LocalDateTime.now() : dto.dateMouvement);
        m.setDatePeremption(dto.datePeremption);
        m.setReference(videSiBlank(dto.reference));
        m.setNotes(videSiBlank(dto.notes));
        m.setUtilisateur(auditService.utilisateurCourant(auteur));
        if (dto.fournisseurId != null) {
            Fournisseur f = fournisseurRepository.findById(dto.fournisseurId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Fournisseur introuvable."));
            m.setFournisseur(f);
        }

        m = mouvementRepository.save(m);

        if (dto.type == TypeMouvementStock.ENTREE && dto.prixUnitaire != null) {
            article.setPrixAchatDernier(dto.prixUnitaire);
            articleRepository.save(article);
        }

        auditService.enregistrer(auteur, "CREATION", "mouvement_stock", m.getId(),
                dto.type + " de " + m.getQuantite() + " sur " + article.getNom());
        return m.versDTO();
    }

    private void validerArticle(ArticleDTO dto) {
        if (dto.nom == null || dto.nom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
    }

    private void appliquerArticle(Article a, ArticleDTO dto) {
        a.setNom(dto.nom.trim());
        a.setMarque(videSiBlank(dto.marque));
        a.setUnite(dto.unite == null || dto.unite.isBlank() ? "unité" : dto.unite.trim());
        a.setSeuilAlerte(dto.seuilAlerte == null ? BigDecimal.ZERO : dto.seuilAlerte);
        a.setNotes(videSiBlank(dto.notes));
        a.setActif(dto.actif);
        if (dto.categorieId != null) {
            CategorieArticle cat = categorieRepository.findById(dto.categorieId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Catégorie introuvable."));
            a.setCategorie(cat);
        } else {
            a.setCategorie(null);
        }
    }

    private void appliquerFournisseur(Fournisseur f, FournisseurDTO dto) {
        f.setNom(dto.nom.trim());
        f.setContact(videSiBlank(dto.contact));
        f.setTelephone(videSiBlank(dto.telephone));
        f.setEmail(videSiBlank(dto.email));
        f.setAdresse(videSiBlank(dto.adresse));
        f.setNotes(videSiBlank(dto.notes));
        f.setActif(dto.actif);
    }

    private static String videSiBlank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
