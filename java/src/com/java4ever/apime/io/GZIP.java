//#condition mm.gzip
/*
 * GZIP library for j2me applications.
 *
 * Copyright (c) 2004-2006 Carlos Araiz (caraiz@java4ever.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Modified by Marc Capdevielle <marc.capdevielle@gmail.com>:
 *  - Comments have been translated to English
 *  - Code has been reformated to match the MeMo coding style
 *  - Small optimizations have been added (prevent some buffers reallocation)
 * 
 */
package com.java4ever.apime.io;

import java.io.*;

/**
 * Class for decompressing GZIP data
 * 
 * @author Carlos Araiz
 * 
 * @version 1.2.0
 */
public class GZIP {
    // Mask for the flags
    private static final int FTEXT_MASK = 1;
    private static final int FHCRC_MASK = 2;
    private static final int FEXTRA_MASK = 4;
    private static final int FNAME_MASK = 8;
    private static final int FCOMMENT_MASK = 16;
    // Types of blocks
    private static final int BTYPE_NONE = 0;
    private static final int BTYPE_FIXED = 1;
    private static final int BTYPE_DYNAMIC = 2;
    private static final int BTYPE_RESERVED = 3;
    // Limits
    private static final int MAX_BITS = 16;
    private static final int MAX_CODE_LITERALS = 287;
    private static final int MAX_CODE_DISTANCES = 31;
    private static final int MAX_CODE_LENGTHS = 18;
    private static final int EOB_CODE = 256;
    // Predefined data (LENGTH: 257..287 / DISTANCE: 0..29 / DYNAMIC_LENGTH_ORDER: 0..18).
    private static final int LENGTH_EXTRA_BITS[] = { 
        0, 0, 0, 0,
        0, 0, 0, 0,
        1, 1, 1, 1, 
        2, 2, 2, 2,
        3, 3, 3, 3,
        4, 4, 4, 4,
        5, 5, 5, 5,
        0, 99, 99
    };
    private static final int LENGTH_VALUES[] = { 
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35,
        43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
    };
    private static final int DISTANCE_EXTRA_BITS[] = { 
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
    };
    private static final int DISTANCE_VALUES[] = { 
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385,
        513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
    };
    private static final int DYNAMIC_LENGTH_ORDER[] = { 
        16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
    };

    // Used for reading compressed data
    private static int gzipIndex, gzipByte, gzipBit;

    /**
     * Descompress a GZIP file
     * 
     * @param gzip
     *            Array containing the compressed data
     * 
     * @return Array containing the data
     */
    public static byte[] inflate (byte gzip[]) throws IOException {
        // Initialization.
        gzipIndex = gzipByte = gzipBit = 0;
        // Check header.
        if (readBits (gzip, 16) != 0x8B1F || readBits (gzip, 8) != 8)
            throw new IOException ("Invalid GZIP format");
        // Flag.
        int flg = readBits (gzip, 8);
        // Jump (4) / XFL(1) / OS(1).
        gzipIndex += 6;
        // Jump other compression flags.
        if ((flg & FEXTRA_MASK) != 0) gzipIndex += readBits (gzip, 16);
        if ((flg & FNAME_MASK) != 0) while (gzip[gzipIndex++] != 0);
        if ((flg & FCOMMENT_MASK) != 0) while (gzip[gzipIndex++] != 0);
        if ((flg & FHCRC_MASK) != 0) gzipIndex += 2;
        // Compute final size
        int index = gzipIndex;
        gzipIndex = gzip.length - 4;
        byte uncompressed[] = new byte[readBits (gzip, 16) | (readBits (gzip, 16) << 16)];
        int uncompressedIndex = 0;
        gzipIndex = index;
        // Inflate blocks
        int bfinal = 0, btype = 0;
        // Cached refs to prevent some reapeated new allocations
        int cachedLiteralTree[] = null, cachedDistanceTree[] = null;
        byte cachedLengthBits[] = null;
        do {
            // Read the block header
            bfinal = readBits (gzip, 1);
            btype = readBits (gzip, 2);
            // Check type of compression
            if (btype == BTYPE_NONE) {
                // Ignore bits from the actual byte
                gzipBit = 0;
                // Read LEN & NLEN
                int len = readBits (gzip, 16);
                readBits (gzip, 16); // unused NLEN
                // Read data
                System.arraycopy (gzip, gzipIndex, uncompressed, uncompressedIndex, len);
                gzipIndex += len;
                uncompressedIndex += len;
            } else {
                int literalTree[], distanceTree[];
                if (btype == BTYPE_DYNAMIC) {
                    // Number of data for each type.
                    int hlit = readBits (gzip, 5) + 257;
                    int hdist = readBits (gzip, 5) + 1;
                    int hclen = readBits (gzip, 4) + 4;
                    // Read the number of bits for each LEN code.
                    if (cachedLengthBits == null) {
                        cachedLengthBits = new byte[MAX_CODE_LENGTHS + 1];
                    } else {
                        for (int i = 0; i < MAX_CODE_LENGTHS + 1; i++) {
                            cachedLengthBits[i] = 0;
                        }
                    }
                    for (int i = 0; i < hclen; i++) {
                        cachedLengthBits[DYNAMIC_LENGTH_ORDER[i]] = (byte) readBits (gzip, 3);
                    }
                    // Create codes for length.
                    int lengthTree[] = createHuffmanTree (cachedLengthBits, MAX_CODE_LENGTHS);
                    // Generate the trees.
                    literalTree = createHuffmanTree (decodeCodeLengths (gzip, lengthTree, hlit), hlit - 1);
                    distanceTree = createHuffmanTree (decodeCodeLengths (gzip, lengthTree, hdist), hdist - 1);
                } else {
                    if (cachedLiteralTree == null) { // first time only
                        byte literalBits[] = new byte[MAX_CODE_LITERALS + 1];
                        for (int i = 0; i < 144; i++)   literalBits[i] = 8;
                        for (int i = 144; i < 256; i++) literalBits[i] = 9;
                        for (int i = 256; i < 280; i++) literalBits[i] = 7;
                        for (int i = 280; i < 288; i++) literalBits[i] = 8;
                        cachedLiteralTree = createHuffmanTree (literalBits, MAX_CODE_LITERALS);
                        byte distanceBits[] = new byte[MAX_CODE_DISTANCES + 1];
                        for (int i = 0; i < MAX_CODE_DISTANCES + 1; i++) distanceBits[i] = 5;
                        cachedDistanceTree = createHuffmanTree (distanceBits, MAX_CODE_DISTANCES);
                    }
                    literalTree = cachedLiteralTree;
                    distanceTree = cachedDistanceTree;
                }
                // Decompress block
                int code = 0, leb = 0, deb = 0;
                while ((code = readCode (gzip, literalTree)) != EOB_CODE) {
                    if (code > EOB_CODE) {
                        code -= 257;
                        int length = LENGTH_VALUES[code];
                        if ((leb = LENGTH_EXTRA_BITS[code]) > 0) {
                            length += readBits (gzip, leb);
                        }
                        code = readCode (gzip, distanceTree);
                        int distance = DISTANCE_VALUES[code];
                        if ((deb = DISTANCE_EXTRA_BITS[code]) > 0) {
                            distance += readBits (gzip, deb);
                        }
                        // Repeat the information.
                        int offset = uncompressedIndex - distance;
                        while (distance < length) {
                            System.arraycopy (uncompressed, offset, uncompressed, uncompressedIndex, distance);
                            uncompressedIndex += distance;
                            length -= distance;
                            distance <<= 1;
                        }
                        System.arraycopy (uncompressed, offset, uncompressed, uncompressedIndex, length);
                        uncompressedIndex += length;
                    } else {
                        uncompressed[uncompressedIndex++] = (byte) code;
                    }
                }
            }
        } while (bfinal == 0);
        return uncompressed;
    }

    /**
     * Read a number of bits
     * 
     * @param n
     *            Number of bits to read [0..16]
     */
    private static int readBits (byte gzip[], int n) {
        // Ensures that we have a byte.
        int data = (gzipBit == 0 ? (gzipByte = (gzip[gzipIndex++] & 0xFF)) : (gzipByte >> gzipBit));
        // Read to complete the bits.
        for (int i = (8 - gzipBit); i < n; i += 8) {
            gzipByte = (gzip[gzipIndex++] & 0xFF);
            data |= (gzipByte << i);
        }
        // Adjust actual position
        gzipBit = (gzipBit + n) & 7;
        // Returns the data.
        return (data & ((1 << n) - 1));
    }

    /**
     * Read a code
     */
    private static int readCode (byte gzip[], int tree[]) {
        int node = tree[0];
        while (node >= 0) {
            // Read a byte if necessary
            if (gzipBit == 0)
                gzipByte = (gzip[gzipIndex++] & 0xFF);
            // Go to the appropriate node.
            node = (((gzipByte & (1 << gzipBit)) == 0) ? tree[node >> 16] : tree[node & 0xFFFF]);
            // Adjust actual position
            gzipBit = (gzipBit + 1) & 7;
        }
        return (node & 0xFFFF);
    }

    /**
     * Decodes length codes (used with compressed blocks using dynamic codes).
     */
    private static byte[] decodeCodeLengths (byte gzip[], int lengthTree[], int count) {
        byte bits[] = new byte[count];
        for (int i = 0, code = 0, last = 0; i < count;) {
            code = readCode (gzip, lengthTree);
            if (code >= 16) {
                int repeat = 0;
                if (code == 16) {
                    repeat = 3 + readBits (gzip, 2);
                    code = last;
                } else {
                    if (code == 17) {
                        repeat = 3 + readBits (gzip, 3);
                    } else {
                        repeat = 11 + readBits (gzip, 7);
                    }
                    code = 0;
                }
                while (repeat-- > 0) {
                    bits[i++] = (byte) code;
                }
            } else {
                bits[i++] = (byte) code;
            }
            last = code;
        }
        return bits;
    }

    private static int bl_count[] = new int[MAX_BITS + 1];
    private static int next_code[] = new int[MAX_BITS + 1];
    
    /**
     * Create the Huffman tree
     */
    private static int[] createHuffmanTree (byte bits[], int maxCode) {
        // Number of codes for each code length.
        for (int i = 0; i < MAX_BITS+1; i++) bl_count[i] = 0;
        for (int i = 0; i < bits.length; i++) bl_count[bits[i]]++;
        // Minimum value code for each code length.
        int code = 0;
        bl_count[0] = 0;
        next_code[0] = 0;
        for (int i = 1; i <= MAX_BITS; i++) {
            next_code[i] = code = (code + bl_count[i - 1]) << 1;
        }
        // Generate the tree.
        // Bit 31 => Node (0) or code (1).
        // (Node) bit 16..30 => Index of left node (0 if not).
        // (Node) bit 0..15 => Index of right node (0 if not).
        // (Code) bit 0..15
        int tree[] = new int[(maxCode << 1) + MAX_BITS];
        int treeInsert = 1;
        for (int i = 0; i <= maxCode; i++) {
            int len = bits[i];
            if (len != 0) {
                code = next_code[len]++;
                // Fill the tree
                int node = 0;
                for (int bit = len - 1; bit >= 0; bit--) {
                    int value = code & (1 << bit);
                    // Inserts on the left.
                    if (value == 0) {
                        int left = tree[node] >> 16;
                        if (left == 0) {
                            tree[node] |= (treeInsert << 16);
                            node = treeInsert++;
                        } else {
                            node = left;
                        }
                    } else { // Inserts on the right.
                        int right = tree[node] & 0xFFFF;
                        if (right == 0) {
                            tree[node] |= treeInsert;
                            node = treeInsert++;
                        } else {
                            node = right;
                        }
                    }
                }
                // Insert the code
                tree[node] = 0x80000000 | i;
            }
        }
        return tree;
    }
}