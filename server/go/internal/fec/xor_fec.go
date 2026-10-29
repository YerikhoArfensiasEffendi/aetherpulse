package fec

import (
	"sync"
)

// GroupSize is K (number of data packets per parity packet)
const DefaultGroupSize = 4

// FECGroup stores packets in a single parity group
type FECGroup struct {
	BaseSeq uint32
	Packets map[uint32][]byte
	Parity  []byte
}

// XORFEC handles simple sliding window Forward Error Correction
type XORFEC struct {
	mu        sync.Mutex
	groupSize int
	groups    map[uint32]*FECGroup
}

// NewXORFEC creates an XOR FEC engine
func NewXORFEC(groupSize int) *XORFEC {
	if groupSize <= 0 {
		groupSize = DefaultGroupSize
	}
	return &XORFEC{
		groupSize: groupSize,
		groups:    make(map[uint32]*FECGroup),
	}
}

// GenerateParity computes XOR parity for a slice of data payloads
func GenerateParity(payloads [][]byte) []byte {
	if len(payloads) == 0 {
		return nil
	}

	maxLen := 0
	for _, p := range payloads {
		if len(p) > maxLen {
			maxLen = len(p)
		}
	}

	parity := make([]byte, maxLen)
	for _, p := range payloads {
		for i := 0; i < len(p); i++ {
			parity[i] ^= p[i]
		}
	}

	return parity
}

// RecoverMissingPacket recovers 1 missing packet if K-1 packets and parity are present
func RecoverMissingPacket(presentPayloads [][]byte, parity []byte) []byte {
	maxLen := len(parity)
	for _, p := range presentPayloads {
		if len(p) > maxLen {
			maxLen = len(p)
		}
	}

	recovered := make([]byte, maxLen)
	copy(recovered, parity)

	for _, p := range presentPayloads {
		for i := 0; i < len(p); i++ {
			recovered[i] ^= p[i]
		}
	}

	return recovered
}
