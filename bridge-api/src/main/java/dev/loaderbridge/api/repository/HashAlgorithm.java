package dev.loaderbridge.api.repository;

public enum HashAlgorithm {
    SHA1(40),
    SHA512(128);

    private final int hexadecimalLength;

    HashAlgorithm(int hexadecimalLength) {
        this.hexadecimalLength = hexadecimalLength;
    }

    int hexadecimalLength() {
        return hexadecimalLength;
    }
}
