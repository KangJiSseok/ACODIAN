package com.ibank.axwms.domain.auth.repository;

import com.ibank.axwms.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
