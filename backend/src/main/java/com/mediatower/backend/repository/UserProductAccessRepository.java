package com.mediatower.backend.repository;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.model.User;
import com.mediatower.backend.model.UserProductAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProductAccessRepository extends JpaRepository<UserProductAccess, Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserAndProduct(User user, Product product);
    List<UserProductAccess> findByUser(User user);
    Optional<UserProductAccess> findByUserAndProductId(User user, Long productId);
//    boolean existsByUserUidAndProductId(String userUid, Long productId);
    @Query("SELECT COUNT(upa) > 0 FROM UserProductAccess upa WHERE upa.user.uid = :userUid AND upa.product.id = :productId")
    boolean existsByUserUidAndProductId(@Param("userUid") String userUid, @Param("productId") Long productId);
}




