FROM ubuntu:24.04 AS fetch
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /opt/jev
RUN curl -fsSL -o /tmp/jev-linux-x64.tar.gz https://github.com/feder-cr/jev/releases/download/jevos-v5/jev-linux-x64.tar.gz \
 && echo '1ce77566405514ed2f20d64f8222cf9eba9b714d332f05f6296e0cad84dd7b04  /tmp/jev-linux-x64.tar.gz' | sha256sum -c - \
 && tar -xzf /tmp/jev-linux-x64.tar.gz --strip-components=1 \
 && curl -fsSL -o /tmp/jevos-v5-openvino-int8.zip https://github.com/feder-cr/jev/releases/download/jevos-v5/jevos-v5-openvino-int8.zip \
 && echo '0a1159c31b9c2abc484af9c866cdcfe5279fdf592b368636625c5a795f970acc  /tmp/jevos-v5-openvino-int8.zip' | sha256sum -c - \
 && unzip -q /tmp/jevos-v5-openvino-int8.zip

FROM ubuntu:24.04
WORKDIR /opt/jev
COPY --from=fetch /opt/jev /opt/jev
USER 10001:10001
EXPOSE 8017
CMD ["./jev", "serve", "--host", "0.0.0.0", "--port", "8017", "--threads", "2"]
