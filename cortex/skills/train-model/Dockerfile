FROM python:3.9-buster
COPY --from=redboxoss/scuttle:latest /scuttle /bin/scuttle
ADD requirements.txt .

RUN pip install -r requirements.txt

ENV ENVOY_ADMIN_API=http://localhost:15000
ENV ISTIO_QUIT_API=http://localhost:15020
ENV SCUTTLE_LOGGING=false
WORKDIR /app
ADD src .
RUN chmod 755 /app

ENTRYPOINT [ "python", "/app/main.py"]