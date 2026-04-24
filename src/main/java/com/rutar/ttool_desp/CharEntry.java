package com.rutar.ttool_desp;

import java.nio.*;

// ............................................................................
/// Представлення одиночного символу із *.fnt файлу
/// @author Rutar_Andriy
/// 04.01.2026

public class CharEntry {

private char charC;                                                   // символ
private int  charX;                              // горизонтальний зсув символу
private int  charW;                                           // ширина символу
private int  indL;                               // відступ ліворуч від символу
private int  indR;                              // відступ праворуч від символу

// ============================================================================
/// Конструктор за замовчуванням
/// @param charC символ
/// @param charX горизонтальний зсув символу
/// @param charW ширина символу
/// @param indL відступ ліворуч від символу
/// @param indR відступ праворуч від символу

public CharEntry (char charC, int charX, int charW, int indL, int indR)
  { this.charC = charC;
    this.charX = charX;
    this.charW = charW;
    this.indL = indL;
    this.indR = indR; }

// ============================================================================

public char getChar() { return charC; }
public void setChar (char charCode) { this.charC = charCode; }

// ============================================================================

public int getCharX() { return charX; }
public void setCharX (int charX) { this.charX = charX; }

// ============================================================================

public int getCharW() { return charW; }
public void setCharW (int charW) { this.charW = charW; }

// ============================================================================

public int getIndentLeft() { return indL; }
public void setUnknownOne (int indL) { this.indL = indL; }

// ============================================================================

public int getIndentRight() { return indR; }
public void setUnknownTwo (int indR) { this.indR = indR; }

// ============================================================================

@Override
public String toString()
  { return "\"" + getChar()       + "\" - "
                + getCharX()      + ", "
                + getCharW()      + ", "
                + getIndentLeft() + ", "
                + getIndentRight(); }

// ============================================================================

public byte[] toBytes() {
    
    ByteBuffer buffer = ByteBuffer.allocate(18);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    buffer.putChar(charC);
    buffer.putInt(charX);
    buffer.putInt(charW);
    buffer.putInt(indL);
    buffer.putInt(indR);
    
    return buffer.array();
}

// Кінець класу CharEntry =====================================================

}