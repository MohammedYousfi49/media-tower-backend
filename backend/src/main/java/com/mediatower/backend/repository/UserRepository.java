package com.mediatower.backend.repository;

import com.mediatower.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Collection;
import java.util.Optional;

@Repository // Indique que c'est un composant de persistance
public interface UserRepository extends JpaRepository<User, Long> {
    // Méthode personnalisée pour trouver un utilisateur par son email
    Optional<User> findByEmail(String email);
    Optional<User> findByUid(String uid);


    // Méthode pour vérifier si un email existe déjà
    boolean existsByEmail(String email);

    // --- NOUVELLE MÉTHODE ---
    // Cette requête est plus directe et explicite.
    @Query("SELECT u FROM User u WHERE u.uid = :uid")
    Optional<User> findUserByUid(@Param("uid") String uid);
    List<User> findAllByUidIn(Collection<String> uids);
    Optional<User> findByVerificationToken(String token);



}
