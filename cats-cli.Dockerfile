FROM debian:bookworm-20230919-slim
COPY ./target/cats-runner /usr/local/bin/cats
ENTRYPOINT ["/usr/local/bin/cats"]
