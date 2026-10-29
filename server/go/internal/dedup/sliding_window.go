package dedup

import (
	"sync"
)

const WindowSize uint32 = 64

// SlidingWindowDeduplicator handles O(1) packet de-duplication across dual multipath streams
type SlidingWindowDeduplicator struct {
	mu             sync.Mutex
	maxSeqReceived uint32
	windowMask     uint64
	initialized    bool
	TotalReceived  uint64
	DuplicatesDropped uint64
}

// NewSlidingWindowDeduplicator creates a fresh de-duplicator instance
func NewSlidingWindowDeduplicator() *SlidingWindowDeduplicator {
	return &SlidingWindowDeduplicator{
		maxSeqReceived: 0,
		windowMask:     0,
		initialized:    false,
	}
}

// IsDuplicate returns true if the sequence number has already been seen or is too old
func (d *SlidingWindowDeduplicator) IsDuplicate(seq uint32) bool {
	d.mu.Lock()
	defer d.mu.Unlock()

	d.TotalReceived++

	if !d.initialized {
		d.maxSeqReceived = seq
		d.windowMask = 1
		d.initialized = true
		return false
	}

	if seq > d.maxSeqReceived {
		diff := seq - d.maxSeqReceived
		if diff >= WindowSize {
			d.windowMask = 1
		} else {
			d.windowMask = (d.windowMask << diff) | 1
		}
		d.maxSeqReceived = seq
		return false
	}

	diff := d.maxSeqReceived - seq
	if diff >= WindowSize {
		// Packet arrived too late outside the 64-packet window
		d.DuplicatesDropped++
		return true
	}

	bitPos := uint64(1) << diff
	if (d.windowMask & bitPos) != 0 {
		// Packet has already arrived on the faster interface
		d.DuplicatesDropped++
		return true
	}

	// First time seeing this packet from the slower interface
	d.windowMask |= bitPos
	return false
}

// GetStats returns current deduplication statistics
func (d *SlidingWindowDeduplicator) GetStats() (total uint64, dropped uint64) {
	d.mu.Lock()
	defer d.mu.Unlock()
	return d.TotalReceived, d.DuplicatesDropped
}
