#!/usr/bin/env python3
"""
BridgePlay Multipath Edge Relay Server (AsyncIO Python 3)
Standalone, zero external dependencies.
"""

import asyncio
import socket
import struct
import time
from typing import Dict, Tuple

MAGIC_HEADER = 0x4250  # "BP"
HEADER_FORMAT = ">HBBHI"  # Magic (2), Type (1), Flags (1), Length (2), Seq (4) = 10 bytes
HEADER_SIZE = struct.calcsize(HEADER_FORMAT)

TYPE_DATA = 0x01
TYPE_FEC_PARITY = 0x02
TYPE_HEARTBEAT = 0x03

WINDOW_SIZE = 64


class SlidingWindowDeduplicator:
    def __init__(self):
        self.max_seq_received = 0
        self.window_mask = 0
        self.initialized = False
        self.total_received = 0
        self.duplicates_dropped = 0

    def is_duplicate(self, seq: int) -> bool:
        self.total_received += 1
        if not self.initialized:
            self.max_seq_received = seq
            self.window_mask = 1
            self.initialized = True
            return False

        if seq > self.max_seq_received:
            diff = seq - self.max_seq_received
            if diff >= WINDOW_SIZE:
                self.window_mask = 1
            else:
                self.window_mask = (self.window_mask << diff) | 1
            self.max_seq_received = seq
            return False

        diff = self.max_seq_received - seq
        if diff >= WINDOW_SIZE:
            self.duplicates_dropped += 1
            return True

        bit_pos = 1 << diff
        if (self.window_mask & bit_pos) != 0:
            self.duplicates_dropped += 1
            return True

        self.window_mask |= bit_pos
        return False


class RelaySession:
    def __init__(self, client_addr: Tuple[str, int], target_host: str, target_port: int, loop: asyncio.AbstractEventLoop):
        self.client_addr = client_addr
        self.target_host = target_host
        self.target_port = target_port
        self.deduplicator = SlidingWindowDeduplicator()
        self.last_active = time.time()
        self.loop = loop
        self.target_transport = None

    async def init_target_connection(self, on_target_data_callback):
        class TargetProtocol(asyncio.DatagramProtocol):
            def datagram_received(inner_self, data, addr):
                on_target_data_callback(data, self.client_addr)

            def error_received(inner_self, exc):
                pass

        transport, _ = await self.loop.create_datagram_endpoint(
            lambda: TargetProtocol(),
            remote_addr=(self.target_host, self.target_port)
        )
        self.target_transport = transport


class BridgePlayRelayServerProtocol(asyncio.DatagramProtocol):
    def __init__(self, target_host: str, target_port: int):
        self.target_host = target_host
        self.target_port = target_port
        self.transport = None
        self.sessions: Dict[str, RelaySession] = {}
        self.loop = asyncio.get_event_loop()

    def connection_made(self, transport):
        self.transport = transport
        # Set 16MB socket buffer if supported by OS
        sock = transport.get_extra_info("socket")
        if sock:
            try:
                sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 16 * 1024 * 1024)
                sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 16 * 1024 * 1024)
            except OSError:
                pass
        print(f"BridgePlay Python Edge Relay listening for client packets.")

    def datagram_received(self, data: bytes, addr: Tuple[str, int]):
        if len(data) < HEADER_SIZE:
            return

        magic, pkt_type, flags, payload_len, seq = struct.unpack_from(HEADER_FORMAT, data)
        if magic != MAGIC_HEADER:
            return

        payload = data[HEADER_SIZE:HEADER_SIZE + payload_len]

        client_ip = addr[0]
        if client_ip not in self.sessions:
            session = RelaySession(addr, self.target_host, self.target_port, self.loop)
            self.sessions[client_ip] = session
            asyncio.create_task(session.init_target_connection(self.forward_target_to_client))
        else:
            session = self.sessions[client_ip]
            session.client_addr = addr

        session.last_active = time.time()

        if session.deduplicator.is_duplicate(seq):
            # Duplicate straggler discarded instantly
            return

        # Forward winning packet to target server
        if pkt_type == TYPE_DATA and payload and session.target_transport:
            session.target_transport.sendto(payload)

    def forward_target_to_client(self, data: bytes, client_addr: Tuple[str, int]):
        resp_header = struct.pack(HEADER_FORMAT, MAGIC_HEADER, TYPE_DATA, 0, len(data), 0)
        self.transport.sendto(resp_header + data, client_addr)


async def main():
    import argparse
    parser = argparse.ArgumentParser(description="BridgePlay Multipath Edge Relay")
    parser.add_argument("--listen-port", type=int, default=8910, help="Listen UDP port")
    parser.add_argument("--target-host", type=str, default="127.0.0.1", help="Target game host")
    parser.add_argument("--target-port", type=int, default=30000, help="Target game port")
    args = parser.parse_args()

    loop = asyncio.get_running_loop()
    transport, protocol = await loop.create_datagram_endpoint(
        lambda: BridgePlayRelayServerProtocol(args.target_host, args.target_port),
        local_addr=("0.0.0.0", args.listen_port)
    )

    print(f"BridgePlay Edge Relay Server started on port {args.listen_port}")
    try:
        await asyncio.Future()
    except asyncio.CancelledError:
        transport.close()


if __name__ == "__main__":
    asyncio.run(main())
