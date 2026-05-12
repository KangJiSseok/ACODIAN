from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda

from app.client.gemini_client import get_gemini_client
from app.prompt.summary_prompt import WORKLOG_SUMMARY_SYSTEM, WORKLOG_SUMMARY_USER


def create_summary_chain():
    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", WORKLOG_SUMMARY_SYSTEM),
            ("human", WORKLOG_SUMMARY_USER),
        ]
    )

    async def generate_summary(prompt_value: object) -> str:
        prompt_text = (
            prompt_value.to_string()
            if hasattr(prompt_value, "to_string")
            else str(prompt_value)
        )
        return await get_gemini_client().generate_text(prompt_text)

    return prompt | RunnableLambda(generate_summary) | StrOutputParser()
