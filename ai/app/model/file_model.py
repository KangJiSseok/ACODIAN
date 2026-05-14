"""파일 AI 요약 트리거/응답 모델.

api 의 TriggerFileSummaryDto.Request 와 필드명이 1:1 매칭되어야 한다
(camelCase alias 로 받음).
"""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class FileSummaryRequest(BaseModel):
    """``POST /summarize/files`` 페이로드.

    api 의 ``WorklogFileAiSummaryTrigger`` 가 AFTER_COMMIT 단계에서 fire-and-forget 으로 호출한다.
    """

    model_config = ConfigDict(populate_by_name=True)

    file_id: int = Field(alias="fileId", ge=1)
    worklog_id: int = Field(alias="worklogId", ge=1)
    storage_key: str = Field(alias="storageKey", min_length=1)
    original_name: str = Field(alias="originalName", min_length=1)
    file_extension: str = Field(alias="fileExtension", min_length=1)


class FileSummaryAcceptedResponse(BaseModel):
    """트리거 즉시 반환되는 비동기 수락 응답."""

    model_config = ConfigDict(populate_by_name=True)

    file_id: int = Field(alias="fileId")
    summary_task_id: str = Field(alias="summaryTaskId")
    status: Literal["ACCEPTED"] = "ACCEPTED"
