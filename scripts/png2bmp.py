#!/usr/bin/env python3
"""PNG (8-bit RGB) -> Allwinner /oem/bootlogo.bmp (Windows 3.x 24-bit, bottom-up BGR)."""
import struct
import sys
import zlib


def _paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def decode_png(data):
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    i = 8
    w = h = None
    idat = []
    while i < len(data):
        ln = struct.unpack(">I", data[i : i + 4])[0]
        typ = data[i + 4 : i + 8]
        chunk = data[i + 8 : i + 8 + ln]
        if typ == b"IHDR":
            w, h, bit, ct, comp, filt, inter = struct.unpack(">IIBBBBB", chunk)
            if (bit, ct, comp, filt, inter) != (8, 2, 0, 0, 0):
                raise ValueError("need 8-bit RGB non-interlaced PNG")
        elif typ == b"IDAT":
            idat.append(chunk)
        elif typ == b"IEND":
            break
        i += 12 + ln
    raw = zlib.decompress(b"".join(idat))
    bpp = 3
    stride = w * bpp
    prev = bytearray(stride)
    rows = []
    o = 0
    for _ in range(h):
        f = raw[o]
        o += 1
        row = bytearray(raw[o : o + stride])
        o += stride
        if f == 1:
            for x in range(stride):
                row[x] = (row[x] + (row[x - bpp] if x >= bpp else 0)) & 255
        elif f == 2:
            for x in range(stride):
                row[x] = (row[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                left = row[x - bpp] if x >= bpp else 0
                row[x] = (row[x] + ((left + prev[x]) // 2)) & 255
        elif f == 4:
            for x in range(stride):
                left = row[x - bpp] if x >= bpp else 0
                up_left = prev[x - bpp] if x >= bpp else 0
                row[x] = (row[x] + _paeth(left, prev[x], up_left)) & 255
        elif f != 0:
            raise ValueError("png filter %d" % f)
        rows.append(bytes(row))
        prev = row
    return w, h, rows


def write_bmp(path, w, h, rows):
    rowb = w * 3
    pad = (4 - (rowb % 4)) % 4
    img = (rowb + pad) * h
    hdr = 54
    with open(path, "wb") as f:
        f.write(b"BM")
        f.write(struct.pack("<IHHI", hdr + img, 0, 0, hdr))
        f.write(struct.pack("<IIIHHIIIIII", 40, w, h, 1, 24, 0, img, 3780, 3780, 0, 0))
        for y in range(h - 1, -1, -1):
            rgb = rows[y]
            bgr = bytearray(rowb)
            for x in range(0, rowb, 3):
                bgr[x], bgr[x + 1], bgr[x + 2] = rgb[x + 2], rgb[x + 1], rgb[x]
            f.write(bgr)
            if pad:
                f.write(b"\x00" * pad)


def _self_check():
    # 2x1 RGB, filter None: red then green
    raw = bytes([0, 255, 0, 0, 0, 255, 0])
    ihdr = struct.pack(">IIBBBBB", 2, 1, 8, 2, 0, 0, 0)

    def chunk(typ, data):
        crc = zlib.crc32(typ + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + typ + data + struct.pack(">I", crc)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    w, h, rows = decode_png(png)
    assert (w, h, rows) == (2, 1, [bytes([255, 0, 0, 0, 255, 0])])
    import os
    import tempfile
    fd, path = tempfile.mkstemp(suffix=".bmp")
    os.close(fd)
    try:
        write_bmp(path, w, h, rows)
        bmp = open(path, "rb").read()
    finally:
        os.remove(path)
    assert bmp[:2] == b"BM"
    # bottom-up BGR: red=(0,0,255), green=(0,255,0) plus 2-byte pad
    assert bmp[54:60] == bytes([0, 0, 255, 0, 255, 0])
    print("png2bmp self-check ok")


def main():
    if len(sys.argv) == 2 and sys.argv[1] == "--self-check":
        _self_check()
        return
    if len(sys.argv) != 3:
        sys.exit("usage: png2bmp.py in.png out.bmp")
    w, h, rows = decode_png(open(sys.argv[1], "rb").read())
    write_bmp(sys.argv[2], w, h, rows)
    print("%dx%d -> %s" % (w, h, sys.argv[2]))


if __name__ == "__main__":
    main()
