package com.mediatower.backend.repository;

import com.mediatower.backend.model.Review;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Trouver tous les avis pour un produit donné
    List<Review> findByProduct(Product product);

    // Trouver tous les avis donnés par un utilisateur spécifique
    List<Review> findByUser(User user);

    // Trouver un avis spécifique d'un utilisateur pour un produit
    Optional<Review> findByUserAndProduct(User user, Product product);
}