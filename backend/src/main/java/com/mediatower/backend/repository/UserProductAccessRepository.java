package com.mediatower.backend.repository;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.model.User;
import com.mediatower.backend.model.UserProductAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProductAccessRepository extends JpaRepository<UserProductAccess, Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserAndProduct(User user, Product product);
    List<UserProductAccess> findByUser(User user);
    Optional<UserProductAccess> findByUserAndProductId(User user, Long productId);


}