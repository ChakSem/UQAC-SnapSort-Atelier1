from vllm import LLM
from vllm.sampling_params import SamplingParams

model_name = "mistralai/Pixtral-12B-2409"

sampling_params = SamplingParams(max_tokens=2048)

llm = LLM(model=model_name, tokenizer_mode="mistral")

prompt = "Describe this image in one sentence."
image_url = "image.png"

messages = [
    {
        "role": "user",
        "content": [{"type": "text", "text": prompt}, {"type": "image_url", "image_url": {"url": image_url}}]
    },
]

outputs = llm.chat(messages, sampling_params=sampling_params)

print(outputs[0].outputs[0].text)
