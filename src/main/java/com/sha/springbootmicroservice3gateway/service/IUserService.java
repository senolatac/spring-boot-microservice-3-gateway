package com.sha.springbootmicroservice3gateway.service;

import com.sha.springbootmicroservice3gateway.model.User;

import java.util.List;
import java.util.Optional;

/**
 * @author sa
 * @date 18.04.2021
 * @time 12:11
 */
public interface IUserService
{
    User saveUser(User user);

    Optional<User> findBUsername(String username);

    List<User> findAllUsers();
}
