package com.rutar.ttool_desp;

// ............................................................................

import java.io.*;
import java.nio.charset.*;
import org.apache.commons.compress.compressors.bzip2.*;
import org.apache.commons.compress.compressors.deflate.*;

import static java.lang.System.*;

/// Корисні допоміжні методи
/// @author Rutar_Andriy
/// 01.01.2026

public class Utils {

// ============================================================================
/// Перетворення кольору RGB565 в колір RGB888
/// @param rgb565 колір у RGB565 представленні
/// @return колір у RGB888 представленні

public static int from565to888rgb (short rgb565) {
    
    int r5 = (rgb565 >> 11) & 0x1F; // 5 байт
    int g6 = (rgb565 >> 5)  & 0x3F; // 6 байт
    int b5 =  rgb565        & 0x1F; // 5 байт
    
    int r8 = (r5 * 255) / 31;       // 8 байт
    int g8 = (g6 * 255) / 63;       // 8 байт
    int b8 = (b5 * 255) / 31;       // 8 байт
    
    return (r8 << 16) | (g8 << 8) | b8;

}

// ============================================================================
/// Перетворення кольору RGB888 в колір RGB565
/// @param rgb888 колір у RGB888 представленні
/// @return колір у RGB565 представленні

public static short from888to565rgb (int rgb888) {
    
    int r8 = (rgb888 >> 16) & 0xFF; // 8 байт
    int g8 = (rgb888 >> 8)  & 0xFF; // 8 байт
    int b8 =  rgb888        & 0xFF; // 8 байт
    
    int r5 = (r8 * 31 + 127) / 255; // 5 байт
    int g6 = (g8 * 63 + 127) / 255; // 6 байт
    int b5 = (b8 * 31 + 127) / 255; // 5 байт
    
    return (short) ((r5 << 11) | (g6 << 5) | b5);

}

// ============================================================================
/// Отримання коду символу в кодуванні cp1251
/// @param c символ
/// @return код символу в кодуванні cp1251

public static int fromCP1251CharToCode (char c) {
    
    return String.valueOf(c).getBytes(Charset.forName("cp1251"))[0] & 0xFF;
}

// ============================================================================
/// Отримання символу за його кодом в кодуванні cp1251
/// @param code код символу в кодуванні cp1251
/// @return відповідний символ

public static char fromCodeToCP1251Char (int code) {
    
    byte bCode = (byte) code;
    return new String(new byte[]{bCode}, Charset.forName("cp1251")).charAt(0);
}

// ============================================================================
/// Перетворення символу на рядок
/// @param c символ для перетворення
/// @return рядкове представлення символу

public static String fromCharToString (char c) {
    
    String result = String.valueOf(c);

    // Обробка усіх символів, які не можна використовувати в іменах 
    // файлів на Windows (\ / : * ? " < > |), а також символу "_"
    if (result.equals("\\") || result.equals("/")  ||
        result.equals(":")  || result.equals("*")  ||
        result.equals("?")  || result.equals("\"") ||
        result.equals("<")  || result.equals(">")  ||
        result.equals("|")  || result.equals("_")) {
        
        result = Integer.toString(Utils.fromCP1251CharToCode(c));
    
    }
    
    return result;

}

// ============================================================================
/// Перетворення рядка на символ
/// @param s рядок для перетворення
/// @return символьне представлення рядка

public static char fromStringToChar (String s) {
    
    if (s.length() == 1) { return s.charAt(0); }
    else { return fromCodeToCP1251Char(Integer.parseInt(s)); }  
}

// ============================================================================
/// Розпакування даних, запакованих з допомогою алгоритму zlib
/// @param compressed дані, запаковані з допомогою алгоритму zlib
/// @return розпаковані дані

public static byte[] zlibDecompress (byte[] compressed) {

try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
     DeflateCompressorInputStream dis = new DeflateCompressorInputStream(bais);
     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

    int n;
    byte[] byteBuffer = new byte[8192];
    
    while ((n = dis.read(byteBuffer)) != -1) { baos.write(byteBuffer, 0, n); }
    
    return baos.toByteArray();
}

catch (Exception e)
    { err.println("zlib decompress error");
      return null; }

}

// ============================================================================
/// Запакування даних з допомогою алгоритму bzip2
/// @param decompressed незапаковані дані
/// @return запаковані дані

public static byte[] bzip2Compress (byte[] decompressed) {

ByteArrayOutputStream baos = new ByteArrayOutputStream();

try (BZip2CompressorOutputStream bzos = new BZip2CompressorOutputStream(baos))
    { bzos.write(decompressed); }

catch (Exception e)
    { err.println("bzip2 compress error");
      return null; }

return baos.toByteArray();

}

// ============================================================================
/// Розпакування даних, запакованих з допомогою алгоритму bzip2
/// @param compressed дані, запаковані з допомогою алгоритму bzip2
/// @return розпаковані дані

public static byte[] bzip2Decompress (byte[] compressed) {

try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
     BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(bais);
     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

    int n;
    byte[] byteBuffer = new byte[8192];
    
    while ((n = bzis.read(byteBuffer)) != -1) { baos.write(byteBuffer, 0, n); }
    
    return baos.toByteArray();
}

catch (Exception e)
    { err.println("bzip2 decompress error");
      return null; }

}

// Кінець класу Utils =========================================================

}
