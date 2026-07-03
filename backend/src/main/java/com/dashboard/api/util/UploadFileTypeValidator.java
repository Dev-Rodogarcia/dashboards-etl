package com.dashboard.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public final class UploadFileTypeValidator {

    private static final int SNIFF_BYTES = 512;
    private static final byte[] XLS_MAGIC = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    private UploadFileTypeValidator() {
    }

    public static void validarAssinatura(
            MultipartFile arquivo,
            Set<FileType> tiposPermitidos,
            String mensagemFormatoInvalido
    ) {
        Objects.requireNonNull(tiposPermitidos, "tiposPermitidos");
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(mensagemFormatoInvalido);
        }

        byte[] assinatura = lerAssinatura(arquivo);
        FileType detectado = detectar(assinatura);
        if (!tiposPermitidos.contains(detectado)) {
            throw new IllegalArgumentException(mensagemFormatoInvalido);
        }
    }

    public static Set<FileType> tipos(FileType primeiro, FileType... restantes) {
        EnumSet<FileType> tipos = EnumSet.of(primeiro);
        tipos.addAll(Arrays.asList(restantes));
        return tipos;
    }

    private static byte[] lerAssinatura(MultipartFile arquivo) {
        try (InputStream inputStream = arquivo.getInputStream()) {
            return inputStream.readNBytes(SNIFF_BYTES);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível validar o conteúdo do arquivo enviado.");
        }
    }

    private static FileType detectar(byte[] bytes) {
        if (isXlsx(bytes)) {
            return FileType.XLSX;
        }
        if (isXls(bytes)) {
            return FileType.XLS;
        }
        if (isTextoCsv(bytes)) {
            return FileType.CSV;
        }
        return FileType.DESCONHECIDO;
    }

    private static boolean isXlsx(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == 0x50
                && bytes[1] == 0x4B
                && (bytes[2] == 0x03 || bytes[2] == 0x05 || bytes[2] == 0x07)
                && (bytes[3] == 0x04 || bytes[3] == 0x06 || bytes[3] == 0x08);
    }

    private static boolean isXls(byte[] bytes) {
        if (bytes.length < XLS_MAGIC.length) {
            return false;
        }
        for (int index = 0; index < XLS_MAGIC.length; index++) {
            if (bytes[index] != XLS_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTextoCsv(byte[] bytes) {
        if (bytes.length == 0 || isAssinaturaBinariaConhecida(bytes)) {
            return false;
        }

        for (byte b : bytes) {
            int value = b & 0xFF;
            boolean controlePermitido = value == '\t' || value == '\n' || value == '\r';
            if (value == 0 || (value < 0x20 && !controlePermitido)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssinaturaBinariaConhecida(byte[] bytes) {
        String prefixo = new String(bytes, 0, Math.min(bytes.length, 8), java.nio.charset.StandardCharsets.ISO_8859_1)
                .toUpperCase(Locale.ROOT);
        return prefixo.startsWith("%PDF-")
                || prefixo.startsWith("MZ")
                || prefixo.startsWith("GIF87A")
                || prefixo.startsWith("GIF89A")
                || prefixo.startsWith("\u0089PNG");
    }

    public enum FileType {
        XLSX,
        XLS,
        CSV,
        DESCONHECIDO
    }
}
