FROM postgres:9.6
RUN mkdir -p /docker-entrypoint-initdb.d/
COPY ./*.sql /docker-entrypoint-initdb.d/
