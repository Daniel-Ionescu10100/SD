FROM nginx:alpine

# Generate a self-signed TLS certificate valid for 365 days
RUN apk add --no-cache openssl && \
    mkdir -p /etc/nginx/ssl && \
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout /etc/nginx/ssl/server.key \
        -out    /etc/nginx/ssl/server.crt \
        -subj   "/C=RO/ST=Cluj/L=Cluj-Napoca/O=UTCN/OU=SD/CN=localhost"

COPY nginx.conf /etc/nginx/nginx.conf
COPY swagger/ /usr/share/nginx/html/docs/
