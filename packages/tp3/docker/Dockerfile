############################################################
# Dockerfile to run an OrientDB (Graph) Container
# for current developed version
############################################################

# The archive is unpacked in a separate stage, so it doesn't stay in a layer of the final image
FROM eclipse-temurin:21-jre AS unpack

COPY orientdb-tp3-*.tar.gz /tmp/orientdb-tp3.tar.gz

RUN mkdir /orientdb \
      && tar -xzf /tmp/orientdb-tp3.tar.gz -C /orientdb --strip-components=1 \
      && mkdir -p /orientdb/log

#overwrite gremlin server config
COPY gremlin-server.yaml /orientdb/config/


FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="OrientDB TinkerPop 3" \
      org.opencontainers.image.description="OrientDB multi-model database server with Gremlin Server" \
      org.opencontainers.image.licenses="Apache-2.0"

# The server runs as this user. A fixed id keeps the ownership of mounted volumes predictable.
RUN groupadd --system --gid 2424 orientdb \
      && useradd --system --uid 2424 --gid orientdb --home-dir /orientdb --no-create-home orientdb

COPY --from=unpack --chown=orientdb:orientdb /orientdb /orientdb

ENV ORIENTDB_HOME=/orientdb
ENV PATH=/orientdb/bin:$PATH

# Size the heap from the container memory limit instead of the fixed 2GB set by server.sh, which
# doesn't fit in small containers; the disk cache is sized from the memory left. Set
# ORIENTDB_OPTS_MEMORY (e.g. "-Xms4G -Xmx4G") to use a fixed heap.
ENV ORIENTDB_OPTS_MEMORY="-XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=50"

# Keep heap dumps out of the image layer, next to the logs
ENV JAVA_OPTS="-XX:HeapDumpPath=/orientdb/log"

# Starts as root to take over volumes written by older images (which ran as root), then runs the
# server as orientdb; see docker-entrypoint.sh
COPY --chmod=755 docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
ENTRYPOINT ["docker-entrypoint.sh"]

VOLUME ["/orientdb/backup", "/orientdb/databases", "/orientdb/config"]

WORKDIR /orientdb

#OrientDb binary
EXPOSE 2424

#OrientDb http
EXPOSE 2480

#gremlin
EXPOSE 8182

# /ping answers without authentication once the HTTP listener is up
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS -o /dev/null http://127.0.0.1:2480/ping || exit 1

# Default command start the server
CMD ["server.sh"]
