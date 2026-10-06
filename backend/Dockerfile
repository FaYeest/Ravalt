# Build Stage
FROM golang:1.24-alpine AS builder

WORKDIR /app

# Install build dependencies
RUN apk add --no-cache git ca-certificates

# Cache dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy source code and build statically linked binary
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o /ravalt-server ./cmd/server

# Final Runtime Stage
FROM alpine:3.21

WORKDIR /app

RUN apk add --no-cache ca-certificates tzdata

# Create non-root user
RUN adduser -D -u 10001 ravalt
USER ravalt

COPY --from=builder /ravalt-server /app/ravalt-server
COPY --from=builder /app/migrations /app/migrations

EXPOSE 8080

ENTRYPOINT ["/app/ravalt-server"]
