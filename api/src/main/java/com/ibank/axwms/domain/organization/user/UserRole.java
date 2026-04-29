package com.ibank.axwms.domain.organization.user;

import java.util.Arrays;
import java.util.Optional;

public enum UserRole {
    DIRECTOR("본부장"),
    DEPT_HEAD("사업부장"),
    TEAM_LEAD("팀장"),
    MEMBER("팀원");

    private final String titleName;

    UserRole(String titleName) {
        this.titleName = titleName;
    }

    public static Optional<UserRole> findByTitleName(String titleName) {
        return Arrays.stream(values())
                .filter(role -> role.titleName.equals(titleName))
                .findFirst();
    }
}
