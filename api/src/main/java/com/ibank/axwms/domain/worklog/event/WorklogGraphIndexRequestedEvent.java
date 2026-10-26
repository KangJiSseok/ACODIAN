package com.ibank.axwms.domain.worklog.event;

/** 커밋 후 graph 색인과 선택적 embedding 교체에 필요한 최소 정보다. */
public record WorklogGraphIndexRequestedEvent(Long worklogId, boolean refreshEmbedding) {}
