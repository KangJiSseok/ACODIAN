package com.ibank.axwms.domain.auth.repository;

import com.ibank.axwms.domain.auth.entity.RefreshToken;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    /** 한 사용자에 속한 모든 활성 세션의 refresh token 을 조회한다. userId secondary index 를 활용한 다중 세션 탐색에 사용된다. */
    List<RefreshToken> findAllByUserId(Long userId);
}
