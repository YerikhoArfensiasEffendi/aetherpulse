package relay

import (
	"fmt"
	"log"
	"net"
	"sync"
	"time"

	"github.com/YerikhoArfensiasEffendi/bridgeplay/internal/dedup"
	"github.com/YerikhoArfensiasEffendi/bridgeplay/internal/protocol"
)

type Session struct {
	ClientAddr  *net.UDPAddr
	TargetConn  *net.UDPConn
	Deduplicator *dedup.SlidingWindowDeduplicator
	LastActive  time.Time
}

type UDPRelayServer struct {
	ListenAddr   string
	DefaultTarget string
	sessions     map[string]*Session
	mu           sync.RWMutex
	listener     *net.UDPConn
	stopChan     chan struct{}
}

func NewUDPRelayServer(listenAddr string, defaultTarget string) *UDPRelayServer {
	return &UDPRelayServer{
		ListenAddr:   listenAddr,
		DefaultTarget: defaultTarget,
		sessions:     make(map[string]*Session),
		stopChan:     make(chan struct{}),
	}
}

func (s *UDPRelayServer) Start() error {
	addr, err := net.ResolveUDPAddr("udp4", s.ListenAddr)
	if err != nil {
		return fmt.Errorf("failed to resolve listen addr: %w", err)
	}

	conn, err := net.ListenUDP("udp4", addr)
	if err != nil {
		return fmt.Errorf("failed to listen UDP: %w", err)
	}
	s.listener = conn

	// 16MB socket receive buffer for extreme throughput without drops
	_ = conn.SetReadBuffer(16 * 1024 * 1024)
	_ = conn.SetWriteBuffer(16 * 1024 * 1024)

	log.Printf("BridgePlay Edge Relay Server active on %s (Target default: %s)", s.ListenAddr, s.DefaultTarget)

	go s.cleanupInactiveSessions()

	buf := make([]byte, 65535)
	for {
		select {
		case <-s.stopChan:
			return nil
		default:
			n, clientAddr, err := conn.ReadFromUDP(buf)
			if err != nil {
				continue
			}

			packetData := make([]byte, n)
			copy(packetData, buf[:n])
			go s.handleClientPacket(packetData, clientAddr)
		}
	}
}

func (s *UDPRelayServer) handleClientPacket(data []byte, clientAddr *net.UDPAddr) {
	pkt, err := protocol.Decode(data)
	if err != nil {
		// Passthrough or invalid packet
		return
	}

	sessionKey := clientAddr.IP.String()
	s.mu.Lock()
	session, exists := s.sessions[sessionKey]
	if !exists {
		targetAddr, err := net.ResolveUDPAddr("udp4", s.DefaultTarget)
		if err != nil {
			s.mu.Unlock()
			return
		}

		targetConn, err := net.DialUDP("udp4", nil, targetAddr)
		if err != nil {
			s.mu.Unlock()
			return
		}

		session = &Session{
			ClientAddr:  clientAddr,
			TargetConn:  targetConn,
			Deduplicator: dedup.NewSlidingWindowDeduplicator(),
			LastActive:  time.Now(),
		}
		s.sessions[sessionKey] = session
		go s.handleTargetResponses(session)
	}
	session.ClientAddr = clientAddr
	session.LastActive = time.Now()
	s.mu.Unlock()

	// Check for duplicates
	if session.Deduplicator.IsDuplicate(pkt.SequenceNum) {
		// Straggler copy arrived on the slower network interface: silently discarded
		return
	}

	// Forward the winning datagram to the destination game server
	if pkt.PacketType == protocol.TypeData && len(pkt.Payload) > 0 {
		_, _ = session.TargetConn.Write(pkt.Payload)
	}
}

func (s *UDPRelayServer) handleTargetResponses(session *Session) {
	buf := make([]byte, 65535)
	for {
		n, err := session.TargetConn.Read(buf)
		if err != nil {
			return
		}

		// Encapsulate return packet to client
		respPkt := protocol.Packet{
			Magic:       protocol.MagicHeader,
			PacketType:  protocol.TypeData,
			Flags:       0,
			PayloadLen:  uint16(n),
			SequenceNum: 0,
			Payload:     buf[:n],
		}
		encoded := respPkt.Encode()
		_, _ = s.listener.WriteToUDP(encoded, session.ClientAddr)
	}
}

func (s *UDPRelayServer) cleanupInactiveSessions() {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-s.stopChan:
			return
		case <-ticker.C:
			s.mu.Lock()
			now := time.Now()
			for key, sess := range s.sessions {
				if now.Sub(sess.LastActive) > 2*time.Minute {
					_ = sess.TargetConn.Close()
					delete(s.sessions, key)
				}
			}
			s.mu.Unlock()
		}
	}
}

func (s *UDPRelayServer) Stop() {
	close(s.stopChan)
	if s.listener != nil {
		_ = s.listener.Close()
	}
}
