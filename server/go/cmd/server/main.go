package main

import (
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/YerikhoArfensiasEffendi/bridgeplay/internal/relay"
)

func main() {
	listenAddr := flag.String("listen", "0.0.0.0:8910", "UDP address to listen for BridgePlay packets")
	targetAddr := flag.String("target", "127.0.0.1:30000", "Default target game server address")
	flag.Parse()

	log.Printf("Starting BridgePlay High Performance Multipath Edge Relay")
	log.Printf("Listening on: %s", *listenAddr)
	log.Printf("Forwarding to: %s", *targetAddr)

	srv := relay.NewUDPRelayServer(*listenAddr, *targetAddr)

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		if err := srv.Start(); err != nil {
			log.Fatalf("Server terminated with error: %v", err)
		}
	}()

	<-sigChan
	log.Println("Received termination signal, shutting down gracefully...")
	srv.Stop()
	log.Println("BridgePlay relay shutdown complete.")
}
