FROM golang:1.21-alpine AS builder

WORKDIR /app
COPY go.mod ./
COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /bridgeplay-server ./cmd/server/main.go

FROM alpine:latest
RUN apk --no-cache add ca-certificates
WORKDIR /root/
COPY --from=builder /bridgeplay-server .

EXPOSE 8910/udp

ENTRYPOINT ["./bridgeplay-server"]
