package com.ibank.axwms.domain.organization.user.repository;

import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.jooq.UserJooqRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>, UserJooqRepository {

    /** 로그인 자격 증명 조회에 사용되는 이메일 단건 탐색. 이메일은 tb_user 의 unique key 이므로 최대 1건이 반환된다. */
    Optional<User> findByEmail(String email);

    /** 이메일 존재 여부 확인. 시드/가입 유스케이스에서 UNIQUE 충돌을 사전에 회피하기 위해 사용한다. */
    boolean existsByEmail(String email);

    /** 특정 역할을 가진 사용자 전체를 조회한다. */
    List<User> findAllByRoleCode(UserRole roleCode);
    /**
     * 특정 역할 사용자를 안정적인 id 오름차순으로 반환한다. */
    List<User> findAllByRoleCodeOrderByIdAsc(UserRole roleCode);
}
