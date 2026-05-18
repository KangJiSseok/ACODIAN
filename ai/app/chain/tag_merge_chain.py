from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda

from app.client.gemini_client import get_gemini_client
from app.prompt.tag_merge_prompt import TAG_MERGE_SYSTEM, TAG_MERGE_USER


def create_tag_merge_chain():
    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", TAG_MERGE_SYSTEM),
            ("human", TAG_MERGE_USER),
        ]
    )

    async def generate_candidates(prompt_value: object) -> str:
        prompt_text = (
            prompt_value.to_string()
            if hasattr(prompt_value, "to_string")
            else str(prompt_value)
        )
        return await get_gemini_client().generate_text(prompt_text)

    return prompt | RunnableLambda(generate_candidates) | JsonOutputParser()
