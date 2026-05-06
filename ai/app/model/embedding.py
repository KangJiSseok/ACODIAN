from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class WorklogEmbeddingRequest(BaseModel):
    """업무일지 임베딩 생성 요청.

    HTTP 요청에서는 camelCase(`worklogId`)를 받고, 파이썬 코드에서는 snake_case
    (`worklog_id`)로 다룬다. `populate_by_name=True`는 두 표기를 모두 허용한다.
    """

    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId", ge=1)


class WorklogEmbeddingAcceptedResponse(BaseModel):
    """임베딩 작업 접수 응답.

    실제 Gemini 호출과 DB 저장은 Celery worker가 비동기로 수행하므로,
    라우터는 작업 ID만 즉시 반환한다.
    """

    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId")
    embedding_task_id: str = Field(alias="embeddingTaskId")
    status: Literal["ACCEPTED"] = "ACCEPTED"


class WorklogEmbeddingResult(BaseModel):
    """임베딩 작업 완료 결과.

    Celery task 결과와 내부 스모크 테스트에서 공통으로 사용하는 작은 결과 모델이다.
    """

    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId")
    chunk_count: int = Field(alias="chunkCount", ge=0)
