from pydantic import BaseModel, ConfigDict, Field


class MetaTag(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    tag_id: int = Field(alias="tagId", ge=1)
    tag_name: str = Field(alias="tagName", min_length=1, max_length=100)


class TaggingChainResult(BaseModel):
    existing_tags: list[str] = Field(default_factory=list)
    new_tags: list[str] = Field(default_factory=list)


class TaggingResult(BaseModel):
    existing_tags: list[str] = Field(default_factory=list)
    new_tags: list[str] = Field(default_factory=list)

    @property
    def tag_names(self) -> list[str]:
        return [*self.existing_tags, *self.new_tags]
