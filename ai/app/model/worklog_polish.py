from pydantic import BaseModel, ConfigDict, Field, field_validator


class WorklogPolishRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    request_content: str | None = Field(default=None, alias="requestContent", max_length=10000)
    work_content: str = Field(alias="workContent", min_length=1, max_length=10000)


class WorklogPolishResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    work_content: str = Field(alias="workContent", min_length=1)


class WorklogTitleRecommendationResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    titles: list[str] = Field(alias="titles", max_length=3)

    @field_validator("titles", mode="before")
    @classmethod
    def normalize_titles(cls, value: object) -> object:
        if not isinstance(value, list):
            return value
        return [str(title).strip() for title in value if str(title).strip()][:3]
