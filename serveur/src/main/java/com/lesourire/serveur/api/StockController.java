package com.lesourire.serveur.api;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.dto.ArticleDTO;
import com.lesourire.commun.dto.CategorieArticleDTO;
import com.lesourire.commun.dto.FournisseurDTO;
import com.lesourire.commun.dto.MouvementStockDTO;
import com.lesourire.serveur.service.StockService;

@RestController
@RequestMapping("/api")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/categories-article")
    public List<CategorieArticleDTO> categories() {
        return stockService.categories();
    }

    @GetMapping("/articles")
    public List<ArticleDTO> articles(
            @RequestParam(name = "recherche", required = false) String recherche,
            @RequestParam(name = "inclureInactifs", defaultValue = "true") boolean inclureInactifs) {
        return stockService.rechercherArticles(recherche, inclureInactifs);
    }

    @PostMapping("/articles")
    public ArticleDTO creerArticle(@RequestBody ArticleDTO dto, Principal principal) {
        return stockService.creerArticle(dto, principal.getName());
    }

    @PutMapping("/articles/{id}")
    public ArticleDTO modifierArticle(@PathVariable Long id, @RequestBody ArticleDTO dto,
            Principal principal) {
        return stockService.modifierArticle(id, dto, principal.getName());
    }

    @GetMapping("/fournisseurs")
    public List<FournisseurDTO> fournisseurs(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return stockService.listerFournisseurs(inclureInactifs);
    }

    @PostMapping("/fournisseurs")
    public FournisseurDTO creerFournisseur(@RequestBody FournisseurDTO dto, Principal principal) {
        return stockService.creerFournisseur(dto, principal.getName());
    }

    @PutMapping("/fournisseurs/{id}")
    public FournisseurDTO modifierFournisseur(@PathVariable Long id,
            @RequestBody FournisseurDTO dto, Principal principal) {
        return stockService.modifierFournisseur(id, dto, principal.getName());
    }

    @GetMapping("/articles/{id}/mouvements")
    public List<MouvementStockDTO> mouvements(@PathVariable Long id) {
        return stockService.mouvementsArticle(id);
    }

    @PostMapping("/mouvements-stock")
    public MouvementStockDTO mouvement(@RequestBody MouvementStockDTO dto, Principal principal) {
        return stockService.enregistrerMouvement(dto, principal.getName());
    }
}
