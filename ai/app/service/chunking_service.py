import re

from app.config.settings import settings
from app.store.embedding_store import WorklogEmbeddingSource


class ChunkingService:
    """업무일지를 임베딩 가능한 텍스트 chunk로 변환한다.

    상태/중요도처럼 자주 바뀌는 값은 chunk 본문에 넣지 않는다. 그런 값은 검색 시
    최신 `tb_worklog`와 join해 필터링해야 재임베딩 부담이 생기지 않는다.
    """

    def create_worklog_chunks(self, source: WorklogEmbeddingSource) -> list[str]:
        """업무일지 원본을 하나 이상의 chunk 문자열로 변환한다."""
        prefix = self._format_prefix(source)
        body = self._format_body(source)
        max_chars = settings.chunk_threshold_chars
        if len(prefix) + len(body) <= max_chars:
            return [f"{prefix}\n{body}".strip()]

        body_limit = max(max_chars - len(prefix) - 2, max_chars // 2)
        body_chunks = self._split_text(body, body_limit)
        return [f"{prefix}\n{chunk}".strip() for chunk in body_chunks]

    def _format_prefix(self, source: WorklogEmbeddingSource) -> str:
        """모든 chunk 앞에 반복해서 붙일 안정적인 검색 문맥을 만든다."""
        lines = [
            f"팀: {source.team_name}",
            f"작성자: {source.author_name}",
            f"작성자 역할: {source.author_role}",
            f"제목: {source.title}",
        ]
        if source.predecessor_titles:
            lines.append(f"선행 업무: {', '.join(source.predecessor_titles)}")
        return "\n".join(lines)

    def _format_body(self, source: WorklogEmbeddingSource) -> str:
        """실제 업무 내용 본문을 검색 친화적인 라벨과 함께 구성한다."""
        lines = []
        if source.request_content:
            lines.append(f"요청/지시: {source.request_content.strip()}")
        lines.append(f"수행 내용: {source.work_content.strip()}")
        return "\n".join(lines)

    def _split_text(self, text: str, max_chars: int) -> list[str]:
        """문단 경계를 우선 보존하면서 긴 본문을 나눈다."""
        paragraphs = [paragraph.strip() for paragraph in text.splitlines() if paragraph.strip()]
        chunks: list[str] = []
        current = ""
        for paragraph in paragraphs:
            for part in self._split_paragraph(paragraph, max_chars):
                if not current:
                    current = part
                    continue
                candidate = f"{current}\n{part}"
                if len(candidate) <= max_chars:
                    current = candidate
                else:
                    chunks.append(current)
                    current = part
        if current:
            chunks.append(current)
        return chunks

    def _split_paragraph(self, paragraph: str, max_chars: int) -> list[str]:
        """문단 하나가 너무 길면 문장 경계를 우선 사용해 다시 나눈다."""
        if len(paragraph) <= max_chars:
            return [paragraph]
        sentences = [
            sentence.strip()
            for sentence in re.split(r"(?<=[.!?。！？다요])\s+", paragraph)
            if sentence.strip()
        ]
        if len(sentences) <= 1:
            return [
                paragraph[index : index + max_chars].strip()
                for index in range(0, len(paragraph), max_chars)
            ]

        chunks: list[str] = []
        current = ""
        for sentence in sentences:
            candidate = f"{current} {sentence}".strip() if current else sentence
            if len(candidate) <= max_chars:
                current = candidate
            else:
                if current:
                    chunks.append(current)
                current = sentence
        if current:
            chunks.append(current)
        return chunks
