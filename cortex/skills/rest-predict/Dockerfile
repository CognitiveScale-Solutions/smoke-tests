FROM python:3.9-buster AS base
ADD requirements.txt .
RUN pip install -r requirements.txt

FROM base
WORKDIR /app
ADD src .
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000", "--workers", "2"]
