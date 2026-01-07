package com.rutar.ttool_desp;

// ............................................................................
/// Представлення одиночного символу із *.fnt файлу
/// @author Rutar_Andriy
/// 04.01.2026

public class CharEntry {

private char charC;  // код символу
private int charX;      // горизонтальний зсув символу
private int charW;      // ширина символу
private int unknownOne; // невідомий параметр 1
private int unknownTwo; // невідомий параметр 2

// ============================================================================
/// Конструктор за замовчування
/// @param charC символ
/// @param charX горизонтальний зсув символу
/// @param charW ширина символу
/// @param unknownOne невідомий параметр 1
/// @param unknownTwo невідомий параметр 2

public CharEntry (char charC, int charX, int charW, 
                  int unknownOne, int unknownTwo) {
    
    this.charC = charC;
    this.charX = charX;
    this.charW = charW;
    this.unknownOne = unknownOne;
    this.unknownTwo = unknownTwo;
}

// ============================================================================

public char getChar() { return charC; }
public void setChar (char charCode) { this.charC = charCode; }

// ............................................................................

public int getCharX() { return charX; }
public void setCharX (int charX) { this.charX = charX; }

// ............................................................................

public int getCharW() { return charW; }
public void setCharW (int charW) { this.charW = charW; }

// ............................................................................

public int getUnknownOne() { return unknownOne; }
public void setUnknownOne (int unknownOne) { this.unknownOne = unknownOne; }

// ............................................................................

public int getUnknownTwo() { return unknownTwo; }
public void setUnknownTwo (int unknownTwo) { this.unknownTwo = unknownTwo; }

// ............................................................................

@Override
public String toString() {
    
       return "\"" + getChar()       + "\" - "
                   + getCharX()      + ", "
                   + getCharW()      + ", "
                   + getUnknownOne() + ", "
                   + getUnknownTwo();
}

// Кінець класу CharEntry =====================================================

}