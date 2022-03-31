FROM python:3.9-buster AS scuttle

COPY --from=redboxoss/scuttle:latest /scuttle /bin/scuttle
ENV ENVOY_ADMIN_API=http://localhost:15000
ENV ISTIO_QUIT_API=http://localhost:15020
ENV SCUTTLE_LOGGING=false

FROM scuttle as base
ADD requirements.txt .

RUN pip install -r requirements.txt

FROM base

WORKDIR /app
ADD src .
RUN chmod 755 /app

ENTRYPOINT [ "python", "/app/main.py"]