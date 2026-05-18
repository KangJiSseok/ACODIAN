from pydantic import BaseModel, ConfigDict, Field


class WorklogPolishRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    request_content: str | None = Field(default=None, alias="requestContent", max_length=10000)
    work_content: str = Field(alias="workContent", min_length=1, max_length=10000)


class WorklogPolishResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    work_content: str = Field(alias="workContent", min_length=1)
