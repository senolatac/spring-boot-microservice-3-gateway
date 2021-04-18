package com.sha.springbootmicroservice3gateway.service;

import com.sha.springbootmicroservice3gateway.model.User;

/**
 * @author sa
 * @date 18.04.2021
 * @time 13:21
 */
public interface IAuthenticationService
{
    String signInAndReturnJWT(User signInRequest);
}
