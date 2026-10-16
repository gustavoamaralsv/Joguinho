#!/usr/bin/env python3
"""Gera efeitos sonoros WAV simples para o Cannon Game (sem dependências externas)."""

from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 22050
RAW_DIR = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "raw"


def write_wav(path: Path, samples: list[float], sample_rate: int = SAMPLE_RATE) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "w") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        frames = bytearray()
        for sample in samples:
            clamped = max(-1.0, min(1.0, sample))
            frames.extend(struct.pack("<h", int(clamped * 32767)))
        wf.writeframes(bytes(frames))


def tone(
    frequency: float,
    duration: float,
    amplitude: float = 0.45,
    decay: float = 4.0,
) -> list[float]:
    n = int(SAMPLE_RATE * duration)
    out: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        envelope = math.exp(-decay * t)
        out.append(amplitude * envelope * math.sin(2 * math.pi * frequency * t))
    return out


def game_over_sound() -> list[float]:
    """Três notas descendentes curtas para o fim de jogo."""
    samples: list[float] = []
    for freq in (440.0, 330.0, 220.0):
        samples.extend(tone(freq, 0.22, amplitude=0.5, decay=5.0))
        samples.extend([0.0] * int(SAMPLE_RATE * 0.04))
    return samples


def main() -> None:
    out = RAW_DIR / "game_over.wav"
    write_wav(out, game_over_sound())
    print(f"Wrote {out}")


if __name__ == "__main__":
    main()
