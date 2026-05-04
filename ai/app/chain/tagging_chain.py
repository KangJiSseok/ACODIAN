from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda

from app.client.gemini_client import get_gemini_client
from app.prompt.tagging_prompt import TAG_SYSTEM, TAG_USER


def create_tagging_chain():
    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", TAG_SYSTEM),
            ("human", TAG_USER),
        ]
    )

    async def generate_tags(prompt_value: object) -> str:
        prompt_text = (
            prompt_value.to_string()
            if hasattr(prompt_value, "to_string")
            else str(prompt_value)
        )
        return await get_gemini_client().generate_text(prompt_text)

    return prompt | RunnableLambda(generate_tags) | JsonOutputParser()
