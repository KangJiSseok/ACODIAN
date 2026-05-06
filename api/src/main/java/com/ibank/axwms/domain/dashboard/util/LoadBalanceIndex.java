package com.ibank.axwms.domain.dashboard.util;

import java.util.Arrays;

/**
 * 부하 편중 지수 계산. 사용자 정의: <strong>1 - Gini</strong>, 즉 1 에 가까울수록 균형, 0 에 가까울수록 편중.
 * 입력값은 음수가 아닌 정수(활성 업무 수 등)를 가정한다.
 *
 * <p>주의: Gini 의 이론적 최댓값이 (n-1)/n 이라, 1-Gini 의 실제 하한은 0 이 아니라 1/n 이다.
 * 부서가 4개면 완전 편중에서도 0.25 가 나오므로 "0 에 가까울수록 편중" 표현은 상대적 비교용으로 해석한다.
 * 절대 0~1 정규화가 필요하면 (1 - gini / ((n-1.0)/n)) 같은 보정이 별도로 필요.
 */
public final class LoadBalanceIndex {

    private LoadBalanceIndex() {
    }

    /**
     * Gini 계수 표준 공식: G = 2*Σ(i*xi) / (n*Σxi) - (n+1)/n  (xi 오름차순 정렬, i=1..n)
     * 0 (완전 균형) ~ (n-1)/n (극단 편중) 범위.
     * 입력 길이가 0 이거나 모든 값이 0 이면 균형으로 간주해 0 을 반환한다.
     */
    public static double gini(int[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        int[] sorted = values.clone();
        Arrays.sort(sorted);
        long total = 0L;
        long weighted = 0L;
        int n = sorted.length;
        for (int i = 0; i < n; i++) {
            int value = sorted[i];
            if (value < 0) {
                throw new IllegalArgumentException("부하 편중 지수 입력은 음수일 수 없습니다.");
            }
            total += value;
            weighted += (long) (i + 1) * value;
        }
        if (total == 0L) {
            return 0.0;
        }
        return (2.0 * weighted) / ((double) n * total) - ((double) n + 1.0) / n;
    }

    /** 1 - Gini. 1 에 가까울수록 균형. [0, 1] 범위로 클램프. */
    public static double balance(int[] values) {
        double idx = 1.0 - gini(values);
        if (idx < 0.0) return 0.0;
        if (idx > 1.0) return 1.0;
        return idx;
    }
}
