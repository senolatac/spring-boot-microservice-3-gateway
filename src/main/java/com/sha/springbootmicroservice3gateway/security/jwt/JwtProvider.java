package com.sha.springbootmicroservice3gateway.security.jwt;

import com.sha.springbootmicroservice3gateway.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sa
 * @date 18.04.2021
 * @time 12:49
 */
@Component
public class JwtProvider implements IJwtProvider
{
    @Value("${authentication.jwt.expiration-in-ms}")
    private Long JWT_EXPIRATION_IN_MS;

    private static final String JWT_TOKEN_PREFIX = "Bearer";
    private static final String JWT_HEADER_STRING = "Authorization";

    private final PrivateKey jwtPrivateKey;
    private final PublicKey jwtPublicKey;

    /*
      First, we require public and private keys for RSA encryption and decryption.
      Hence, below is the tool to generate RSA key online.
      It generates RSA public key as well as the private key of size 512 bit, 1024 bit, 2048 bit,
      3072 bit and 4096 bit with Base64 encoded.
      By default, the private key is generated in PKCS#8 format and the public key is generated in X.509 format.
     */
    public JwtProvider(@Value("${authentication.jwt.private-key}") String jwtPrivateKeyStr,
                       @Value("${authentication.jwt.public-key}") String jwtPublicKeyStr)
    {
        KeyFactory keyFactory = getKeyFactory();

        try
        {
            Base64.Decoder decoder = Base64.getDecoder();
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoder.decode(jwtPrivateKeyStr.getBytes()));
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decoder.decode(jwtPublicKeyStr.getBytes()));

            jwtPrivateKey = keyFactory.generatePrivate(privateKeySpec);
            jwtPublicKey = keyFactory.generatePublic(publicKeySpec);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Invalid key specification", e);
        }
    }

    @Override
    public String generateToken(UserPrincipal authentication)
    {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining());

        return Jwts.builder().setSubject(authentication.getUsername())
                .claim("userId", authentication.getId())
                .claim("roles", authorities)
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_IN_MS))
                .signWith(jwtPrivateKey, SignatureAlgorithm.RS512)
                .compact();
    }

    @Override
    public Authentication getAuthentication(HttpServletRequest request)
    {
        String token = resolveToken(request);
        if (token == null)
        {
            return null;
        }
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtPublicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        List<GrantedAuthority> authorities = Arrays.stream(claims.get("roles").toString().split(","))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetails userDetails = new UserPrincipal(userId, username, null);
        return username != null ? new UsernamePasswordAuthenticationToken(userDetails, null, authorities) : null;
    }

    @Override
    public boolean isTokenValid(HttpServletRequest request)
    {
        String token = resolveToken(request);
        if (token == null)
        {
            return false;
        }
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtPublicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (claims.getExpiration().before(new Date()))
        {
            return false;
        }
        return true;
    }

    private String resolveToken(HttpServletRequest request)
    {
        String bearerToken = request.getHeader(JWT_HEADER_STRING);
        if (bearerToken != null && bearerToken.startsWith(JWT_TOKEN_PREFIX))
        {
            return bearerToken.substring(7);
        }
        return null;
    }

    private KeyFactory getKeyFactory()
    {
        try
        {
            return KeyFactory.getInstance("RSA");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Unknown key generation algorithm", e);
        }
    }
}
