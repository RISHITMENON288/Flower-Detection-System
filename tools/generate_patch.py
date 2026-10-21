#!/usr/bin/env python3
"""
tools/generate_patch.py

Generates an incremental (delta) update patch between an older release package
and a newer release package, along with the accompanying update-manifest.json.

Format:
  Uses the GZDIFF40 standard (BSD-style block matching with GZIP streaming).
  Produces bit-for-bit exact reconstruction of the target package.

Usage:
  python tools/generate_patch.py <old_file> <new_file> [options]

Options:
  --output-dir <dir>       Output directory for generated patch and manifest (default: ./build/updates)
  --version-name <name>    New release version name (e.g. 4.1)
  --version-code <code>    New release version code (e.g. 5)
  --from-version <name>    Old version name (e.g. 4.0)
  --from-code <code>       Old version code (e.g. 4)
  --repo <owner/repo>      GitHub repository (default: RISHITMENON288/Flower-Detection-System)
  --changelog <text>       Release changelog or release notes
  --verify                 Verify the patch by applying it and checking target SHA-256
"""

import os
import sys
import argparse
import struct
import gzip
import io
import hashlib
import json

MAGIC = b"GZDIFF40"
HEADER_SIZE = 32

def sha256_file(filepath):
    """Computes hexadecimal lowercase SHA-256 checksum of a file."""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while True:
            chunk = f.read(65536)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest().lower()

def sha256_bytes(data):
    """Computes hexadecimal lowercase SHA-256 checksum of bytes."""
    return hashlib.sha256(data).hexdigest().lower()

def create_delta(old_bytes, new_bytes, chunk_size=32):
    """
    Computes a binary delta from old_bytes to new_bytes using block matching.
    Outputs the 32-byte header followed by 3 GZIP-compressed streams:
      1. Control block (diff_len, extra_len, seek_offset)
      2. Diff block (byte-by-byte delta modulo 256)
      3. Extra block (new literal bytes)
    """
    old_len = len(old_bytes)
    new_len = len(new_bytes)

    # Index chunks in old_bytes
    old_index = {}
    step = max(8, chunk_size // 2)
    for i in range(0, old_len - chunk_size + 1, step):
        key = old_bytes[i:i + chunk_size]
        if key not in old_index:
            old_index[key] = i

    ctrl_buf = io.BytesIO()
    diff_buf = io.BytesIO()
    extra_buf = io.BytesIO()

    new_pos = 0
    old_pos = 0

    while new_pos < new_len:
        match_pos = -1
        match_len = 0

        chunk = new_bytes[new_pos:new_pos + chunk_size]
        if len(chunk) == chunk_size and chunk in old_index:
            candidate_pos = old_index[chunk]
            m = 0
            while (new_pos + m < new_len and 
                   candidate_pos + m < old_len and 
                   new_bytes[new_pos + m] == old_bytes[candidate_pos + m]):
                m += 1
            if m >= chunk_size:
                match_pos = candidate_pos
                match_len = m

        if match_len > 0:
            diff_slice = bytes((new_bytes[new_pos + i] - old_bytes[match_pos + i]) % 256 for i in range(match_len))
            diff_buf.write(diff_slice)
            seek_offset = match_pos - (old_pos + match_len)
            ctrl_buf.write(struct.pack("<qqq", match_len, 0, seek_offset))
            old_pos = match_pos + match_len
            new_pos += match_len
        else:
            # Advance until next match or EOF
            next_pos = new_pos + 1
            while next_pos < new_len:
                candidate_chunk = new_bytes[next_pos:next_pos + chunk_size]
                if len(candidate_chunk) == chunk_size and candidate_chunk in old_index:
                    break
                next_pos += 1
            extra_slice = new_bytes[new_pos:next_pos]
            extra_buf.write(extra_slice)
            ctrl_buf.write(struct.pack("<qqq", 0, len(extra_slice), 0))
            new_pos = next_pos

    ctrl_gz = gzip.compress(ctrl_buf.getvalue(), compresslevel=6)
    diff_gz = gzip.compress(diff_buf.getvalue(), compresslevel=6)
    extra_gz = gzip.compress(extra_buf.getvalue(), compresslevel=6)

    header = struct.pack("<8sQQQ", MAGIC, len(ctrl_gz), len(diff_gz), new_len)
    return header + ctrl_gz + diff_gz + extra_gz

def apply_delta(old_bytes, patch_bytes):
    """
    Applies patch_bytes to old_bytes, reconstructing the exact new_bytes.
    Used for verifying patch integrity.
    """
    if len(patch_bytes) < HEADER_SIZE:
        raise ValueError("Patch too short")
    magic, ctrl_len, diff_len, new_size = struct.unpack("<8sQQQ", patch_bytes[:HEADER_SIZE])
    if magic != MAGIC:
        raise ValueError(f"Invalid magic: {magic}")

    offset = HEADER_SIZE
    ctrl_data = gzip.decompress(patch_bytes[offset:offset + ctrl_len])
    offset += ctrl_len
    diff_data = gzip.decompress(patch_bytes[offset:offset + diff_len])
    offset += diff_len
    extra_data = gzip.decompress(patch_bytes[offset:])

    ctrl_stream = io.BytesIO(ctrl_data)
    diff_stream = io.BytesIO(diff_data)
    extra_stream = io.BytesIO(extra_data)

    new_buf = bytearray(new_size)
    new_pos = 0
    old_pos = 0
    old_len = len(old_bytes)

    while new_pos < new_size:
        tuple_bytes = ctrl_stream.read(24)
        if not tuple_bytes:
            break
        diff_count, extra_count, seek_offset = struct.unpack("<qqq", tuple_bytes)

        if diff_count > 0:
            diff_block = diff_stream.read(diff_count)
            for i in range(diff_count):
                old_val = old_bytes[old_pos + i] if 0 <= old_pos + i < old_len else 0
                new_buf[new_pos + i] = (old_val + diff_block[i]) % 256
            new_pos += diff_count
            old_pos += diff_count

        if extra_count > 0:
            extra_block = extra_stream.read(extra_count)
            new_buf[new_pos:new_pos + extra_count] = extra_block
            new_pos += extra_count

        old_pos += seek_offset

    if new_pos != new_size:
        raise ValueError(f"Reconstructed size mismatch: {new_pos} != {new_size}")

    return bytes(new_buf)

def main():
    parser = argparse.ArgumentParser(description="Generate delta update patches and release manifests.")
    parser.add_argument("old_apk", help="Path to older reference APK")
    parser.add_argument("new_apk", help="Path to new release APK")
    parser.add_argument("--output-dir", default="./build/updates", help="Output directory")
    parser.add_argument("--version-name", default="4.1", help="Target release version name")
    parser.add_argument("--version-code", type=int, default=5, help="Target release version code")
    parser.add_argument("--from-version", default="4.0", help="Source version name")
    parser.add_argument("--from-code", type=int, default=4, help="Source version code")
    parser.add_argument("--repo", default="RISHITMENON288/Flower-Detection-System", help="GitHub repository owner/name")
    parser.add_argument("--changelog", default="", help="Changelog text")
    parser.add_argument("--verify", action="store_true", help="Verify patch by reconstruction")

    args = parser.parse_args()

    if not os.path.isfile(args.old_apk):
        print(f"Error: Old APK file not found at {args.old_apk}", file=sys.stderr)
        sys.exit(1)
    if not os.path.isfile(args.new_apk):
        print(f"Error: New APK file not found at {args.new_apk}", file=sys.stderr)
        sys.exit(1)

    os.makedirs(args.output_dir, exist_ok=True)

    print(f"[1/4] Reading input packages...")
    with open(args.old_apk, "rb") as f:
        old_bytes = f.read()
    with open(args.new_apk, "rb") as f:
        new_bytes = f.read()

    new_apk_name = os.path.basename(args.new_apk)
    new_apk_sha256 = sha256_bytes(new_bytes)
    old_size = len(old_bytes)
    new_size = len(new_bytes)

    print(f"  Old package: {args.old_apk} ({old_size:,} bytes)")
    print(f"  New package: {args.new_apk} ({new_size:,} bytes, SHA-256: {new_apk_sha256[:12]}...)")

    print(f"[2/4] Generating delta patch...")
    patch_bytes = create_delta(old_bytes, new_bytes)
    patch_size = len(patch_bytes)
    patch_sha256 = sha256_bytes(patch_bytes)
    savings_pct = ((new_size - patch_size) * 100.0) / new_size if new_size > 0 else 0

    patch_file_name = f"patch_v{args.from_version}_to_v{args.version_name}.patch"
    patch_file_path = os.path.join(args.output_dir, patch_file_name)
    with open(patch_file_path, "wb") as f:
        f.write(patch_bytes)

    print(f"  Generated patch: {patch_file_path}")
    print(f"  Patch size: {patch_size:,} bytes (Savings: {savings_pct:.1f}% compared to full {new_size:,} bytes)")
    print(f"  Patch SHA-256: {patch_sha256}")

    if args.verify:
        print(f"[3/4] Verifying patch fidelity...")
        reconstructed = apply_delta(old_bytes, patch_bytes)
        rec_sha = sha256_bytes(reconstructed)
        if rec_sha != new_apk_sha256:
            print(f"Verification FAILED: SHA mismatch ({rec_sha} != {new_apk_sha256})", file=sys.stderr)
            sys.exit(2)
        print("  Verification SUCCESS: Reconstructed package is 100% bit-for-bit identical!")
    else:
        print("[3/4] Skipping verification (--verify not specified)")

    print(f"[4/4] Generating update-manifest.json...")
    manifest_data = {
        "versionName": args.version_name,
        "versionCode": args.version_code,
        "releaseTag": f"v{args.version_name}",
        "releaseUrl": f"https://github.com/{args.repo}/releases/tag/v{args.version_name}",
        "changelog": args.changelog or f"Release v{args.version_name}",
        "fullApk": {
            "fileName": new_apk_name,
            "downloadUrl": f"https://github.com/{args.repo}/releases/download/v{args.version_name}/{new_apk_name}",
            "sizeBytes": new_size,
            "sha256": new_apk_sha256
        },
        "patches": [
            {
                "fromVersionName": args.from_version,
                "fromVersionCode": args.from_code,
                "patchFileName": patch_file_name,
                "downloadUrl": f"https://github.com/{args.repo}/releases/download/v{args.version_name}/{patch_file_name}",
                "patchSizeBytes": patch_size,
                "patchSha256": patch_sha256,
                "targetSha256": new_apk_sha256
            }
        ]
    }

    manifest_path = os.path.join(args.output_dir, "update-manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest_data, f, indent=2)

    print(f"  Saved manifest: {manifest_path}")
    print("\nDelta update assets successfully generated!")

if __name__ == "__main__":
    main()
