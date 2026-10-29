package protocol

import (
	"encoding/binary"
	"errors"
)

const (
	MagicHeader uint16 = 0x4250 // "BP" (BridgePlay)
	HeaderSize  int    = 10

	TypeData      uint8 = 0x01
	TypeFECParity uint8 = 0x02
	TypeHeartbeat uint8 = 0x03

	FlagPathWifi     uint8 = 0x01
	FlagPathCellular uint8 = 0x02
)

var (
	ErrPacketTooShort    = errors.New("packet buffer smaller than header size")
	ErrInvalidMagic      = errors.New("invalid protocol magic bytes")
	ErrPayloadSizeMismatch = errors.New("payload length does not match header length")
)

// Packet represents the encapsulated BridgePlay transport datagram
type Packet struct {
	Magic       uint16
	PacketType  uint8
	Flags       uint8
	PayloadLen  uint16
	SequenceNum uint32
	Payload     []byte
}

// Encode serializes a Packet into a byte slice
func (p *Packet) Encode() []byte {
	buf := make([]byte, HeaderSize+len(p.Payload))
	binary.BigEndian.PutUint16(buf[0:2], p.Magic)
	buf[2] = p.PacketType
	buf[3] = p.Flags
	binary.BigEndian.PutUint16(buf[4:6], uint16(len(p.Payload)))
	binary.BigEndian.PutUint32(buf[6:10], p.SequenceNum)
	if len(p.Payload) > 0 {
		copy(buf[HeaderSize:], p.Payload)
	}
	return buf
}

// Decode deserializes a raw byte buffer into a Packet
func Decode(data []byte) (*Packet, error) {
	if len(data) < HeaderSize {
		return nil, ErrPacketTooShort
	}

	magic := binary.BigEndian.Uint16(data[0:2])
	if magic != MagicHeader {
		return nil, ErrInvalidMagic
	}

	packetType := data[2]
	flags := data[3]
	payloadLen := binary.BigEndian.Uint16(data[4:6])
	seq := binary.BigEndian.Uint32(data[6:10])

	if len(data) < HeaderSize+int(payloadLen) {
		return nil, ErrPayloadSizeMismatch
	}

	payload := make([]byte, payloadLen)
	copy(payload, data[HeaderSize:HeaderSize+int(payloadLen)])

	return &Packet{
		Magic:       magic,
		PacketType:  packetType,
		Flags:       flags,
		PayloadLen:  payloadLen,
		SequenceNum: seq,
		Payload:     payload,
	}, nil
}
