from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.model.tagging_model import MetaTag as MetaTagModel
from app.store.models import MetaTag


class TagStore:
    async def list_active_tags_for_merge(
        self,
        session: AsyncSession,
        *,
        min_usage_count: int,
        limit: int,
    ) -> list[MetaTagModel]:
        result = await session.execute(
            select(
                MetaTag.tag_id,
                MetaTag.tag_name,
                MetaTag.description,
                MetaTag.usage_count,
            )
            .where(MetaTag.is_deleted.is_(False))
            .where(MetaTag.usage_count >= min_usage_count)
            .order_by(MetaTag.usage_count.desc(), MetaTag.tag_name.asc(), MetaTag.tag_id.asc())
            .limit(limit)
        )
        return [
            MetaTagModel(
                tagId=row.tag_id,
                tagName=row.tag_name,
                description=row.description,
                usageCount=row.usage_count,
            )
            for row in result
        ]
