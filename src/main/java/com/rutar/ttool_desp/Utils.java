package com.rutar.ttool_desp;

// ............................................................................

import java.nio.charset.Charset;

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
/// Перетворення кольору RGB555 в колір RGB888
/// @param rgb555 колір у RGB555 представленні
/// @return колір у RGB888 представленні

public static int from555to888rgb (short rgb555) {
    
    int r5 = (rgb555 >> 10) & 0x1F; // 5 байт
    int g5 = (rgb555 >> 5)  & 0x1F; // 6 байт
    int b5 =  rgb555        & 0x1F; // 5 байт
    
    int r8 = (r5 << 3) | (r5 >> 2); // 8 байт
    int g8 = (g5 << 3) | (g5 >> 2); // 8 байт
    int b8 = (b5 << 3) | (b5 >> 2); // 8 байт
    
    return (r8 << 16) | (g8 << 8) | b8;

}

// ============================================================================
/// Перетворення кольору ARGB1555 в колір ARGB8888
/// @param argb1555 колір у ARGB1555 представленні
/// @return колір у RGB8888 представленні

public static int from1555to8888argb (short argb1555) {
    
    int a1 = (argb1555 >> 15) & 0x01; // 1 байт
    int r5 = (argb1555 >> 10) & 0x1F; // 5 байт
    int g5 = (argb1555 >> 5)  & 0x1F; // 6 байт
    int b5 =  argb1555        & 0x1F; // 5 байт
    
    int a8 = a1 == 1 ? 0xFF : 0x00; // 8 байт
    int r8 = (r5 << 3) | (r5 >> 2); // 8 байт
    int g8 = (g5 << 3) | (g5 >> 2); // 8 байт
    int b8 = (b5 << 3) | (b5 >> 2); // 8 байт
    
    return (a8 << 24) | (r8 << 16) | (g8 << 8) | b8;

}

// ============================================================================
/// Отримання коду символу в кодуванні cp1251
/// @param c символ
/// @return код символу в кодуванні cp1251

public static int getCharCP1251Code (char c) {
    
    return String.valueOf(c).getBytes(Charset.forName("cp1251"))[0] & 0xFF;
}

// ============================================================================
/// Перетворення символу на строку
/// @param c символ для перетворення
/// @return строкове представлення символу

public static String getCharAsString (char c) {
    
    String result = String.valueOf(c);

    // Обробка усіх символів, які не можна використовувати в іменах 
    // файлів на Windows (\ / : * ? " < > |), а також символу "_"
    if (result.equals("\\") || result.equals("/")  ||
        result.equals(":")  || result.equals("*")  ||
        result.equals("?")  || result.equals("\"") ||
        result.equals("<")  || result.equals(">")  ||
        result.equals("|")  || result.equals("_")) {
        
        result = Integer.toString(Utils.getCharCP1251Code(c));
    
    }
    
    return result;

}

// Кінець класу Utils =========================================================

}
