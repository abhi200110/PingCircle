package com.pingcircle.pingCircle.repository;

import com.pingcircle.pingCircle.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    
    Optional<Users> findByUsername(String username);
   
    Optional<Users> findByEmail(String email);
    
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM Users u WHERE u.username LIKE %:searchTerm% OR u.name LIKE %:searchTerm%")
    List<Users> searchUsers(@Param("searchTerm") String searchTerm);
}