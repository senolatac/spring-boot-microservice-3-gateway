package com.sha.springbootmicroservice3gateway.repository;

import com.sha.springbootmicroservice3gateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author sa
 * @date 18.04.2021
 * @time 12:08
 */
public interface IUserRepository extends JpaRepository<User, Long>
{
    Optional<User> findByUsername(String username);
}
