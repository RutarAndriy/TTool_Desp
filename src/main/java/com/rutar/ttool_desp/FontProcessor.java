package com.rutar.ttool_desp;

import java.io.*;
import java.awt.*;
import java.nio.*;
import java.util.*;
import javax.imageio.*;
import java.nio.file.*;
import java.awt.image.*;

import static java.io.File.*;
import static java.lang.System.*;
import static javax.swing.JOptionPane.*;
import static java.awt.image.BufferedImage.*;
import static com.rutar.ttool_desp.TToolDesp.*;

// ............................................................................
/// Компіляція та декомпіляція *.fnt файлів
/// @author Rutar_Andriy
/// 08.01.2026

public class FontProcessor {

private byte[] data;
private ByteBuffer mainBuffer, tmpBuffer;

private final TToolDesp mainWindow;

// ============================================================================
/// Конструктор за замовчуванням
/// @param mainWindow головне вікно програми

public FontProcessor (TToolDesp mainWindow) { this.mainWindow = mainWindow; }

// ============================================================================
/// Розпаковування *.fnt файлів
/// @param allBytes усі зчитані байти файлу
/// @param outputFile вихідна папка

public void decompileFont (byte[] allBytes, File outputFile) {

mainBuffer = ByteBuffer.wrap(allBytes);
mainBuffer.order(ByteOrder.LITTLE_ENDIAN);

tmpBuffer = ByteBuffer.allocate(70);
tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);

// ............................................................................
if (debug) { out.println(" --- Font Header --- "); }

data = new byte[6];                                                // Сигнатура
mainBuffer.get(data);
tmpBuffer.put(data);
if (debug) { out.println("Signature: " + new String(data)); }

int aInt = mainBuffer.getInt();                                  // Доп. змінна
tmpBuffer.putInt(aInt);
if (debug) { out.println("Type_1?: " + aInt); }

data = new byte[36];                                            // Назва шрифту
mainBuffer.get(data);
tmpBuffer.put(data);
if (debug) { out.println("Font name: " + new String(data)); }

short aShort = mainBuffer.getShort();                          // Невідомий тип
tmpBuffer.putShort(aShort);
if (debug) { out.println("Type_2?: " + aShort); }

short a2Short = mainBuffer.getShort();                         // Невідомий тип
tmpBuffer.putShort(a2Short);
if (debug) { out.println("Type_3?: " + a2Short); }

int fontHeight = mainBuffer.getInt();                          // Висота шрифту
tmpBuffer.putInt(fontHeight);
if (debug) { out.println("FontHeight: " + fontHeight); }

int leterRectangleWidth = mainBuffer.getInt();   // Ширина символьного квадрату
tmpBuffer.putInt(leterRectangleWidth);
if (debug) { out.println("RectangleWidth: " + leterRectangleWidth); }

int maxLetterWidth = mainBuffer.getInt();         // Максимальна ширина символу
tmpBuffer.putInt(maxLetterWidth);
if (debug) { out.println("MaxLetterWidth: " + maxLetterWidth); }

int entryCount = mainBuffer.getInt();            // Кількість символів у шрифті
tmpBuffer.putInt(entryCount);
if (debug) { out.println("EntryCount: " + entryCount); }

// Перевірка заданої умови
if (aInt >= 512) {
  int a2Int = mainBuffer.getInt();                             // Невідомий тип
  tmpBuffer.putInt(a2Int);
  if (debug) { out.println("Type_4?: " + a2Int); }
}

// ............................................................................
if (debug) { out.println(" --- Chars --- "); }

ArrayList<CharEntry> charEntries = new ArrayList<>();

for (int z = 0; z < entryCount; z++) {

  String num = String.format("%03d", z + 1);
  CharEntry entry = new CharEntry(mainBuffer.getChar(),
                                  mainBuffer.getInt(), mainBuffer.getInt(),
                                  mainBuffer.getInt(), mainBuffer.getInt());

  charEntries.add(entry);

  if (debug) { out.println(num + " - " + entry.toString()); }
}

// ............................................................................
if (debug) { out.println(" --- Images --- "); }

// Створення вихідної папки
outputFile.mkdir();

// Отримання сирих батів заголовку шрифта
tmpBuffer.flip();
byte[] fontHeader = new byte[tmpBuffer.remaining()];
tmpBuffer.get(fontHeader);

// Загальне зображення усіх симолів шрифту
BufferedImage fontImage = null;

// ............................................................................

for (int z = 1; z <= 2; z++) {

int color;
short width  = mainBuffer.getShort(); // ширина зображення шрифта
short height = mainBuffer.getShort(); // висота зображення шрифта
int compType = mainBuffer.getInt();   // тип стиснення даних 
int compSize = mainBuffer.getInt();   // розмір стиснених даних

data = new byte[compSize];
mainBuffer.get(data);

// Розпаковування стиснених даних
data = compType == 1 ? Utils.zlibDecompress(data) :
                       Utils.bzip2Decompress(data);

if (debug) { out.println("Image_" + z + ", " + width + "x" + height
                       + ", comp = " + compType + ", size = " + compSize); }

if (fontImage == null) { fontImage = new BufferedImage(width, height * 2,
                                                       TYPE_3BYTE_BGR); }

// Зчитування розпакованих даних та відтворення зображення зі шрифтом
tmpBuffer = ByteBuffer.wrap(data);
tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);

for (int y = 0; y < height; y++) {
for (int x = 0; x < width;  x++) {
  color = Utils.from565to888rgb(tmpBuffer.getShort());
  fontImage.setRGB(x, height * (z - 1) + y, color);
}
}
}

// ............................................................................
// Запис налагоджувального зображення у файл

if (debug) 
  { File imgFile = new File(outputFile.getParent() + separator
                          + outputFile.getName() + "_in.bmp");
    try { ImageIO.write(fontImage, "bmp", imgFile); }
    catch (IOException e) { showErrorMessage(e); } }

// ............................................................................
// Проходження по всіх символах та запис їх у файли

for (int q = 0; q < charEntries.size(); q++) {
    
  CharEntry entry = charEntries.get(q);
  String num = String.format("%03d", q + 1);

  int imW = leterRectangleWidth + 1;
  int imH = fontHeight * 2;
  int x = entry.getCharX();

  // Обробка можливого виходу за фактичні розміри зображення
  if (x + imW > fontImage.getWidth()) { imW = fontImage.getWidth() - x; }

  // Отримання частинки зображення із конкретним символом
  BufferedImage subimage = fontImage.getSubimage(x, 0, imW, imH);
  String code = Utils.fromCharToString(entry.getChar());

  String name = num                          // порядковий номер символу
              + "_" + code                   // код символу
              + "_" + entry.getCharX()       // горизонтальний зсув символу
              + "_" + entry.getCharW()       // ширина символу
              + "_" + entry.getIndentLeft()  // відступ ліворуч
              + "_" + entry.getIndentRight() // відступ праворуч
              + ".bmp"; 
    
  File entryFile = new File(outputFile.getPath() + separator + name);

  try { ImageIO.write(subimage, "bmp", entryFile); }
  catch (IOException e) { showErrorMessage(e);
                          return; }
}

// ............................................................................
// Запис заголовку шрифта у файл

File headerFile = new File(outputFile.getPath()  + separator
                         + fontImage.getWidth()  + "x"
                         + fontHeight + ".bin");

try (FileOutputStream fis = new FileOutputStream(headerFile))
  { fis.write(fontHeader); }
catch (Exception e) { showErrorMessage(e);
                      return; }

if (debug) { System.out.println(" --- xxx --- "); }

}

// ============================================================================
/// Запапаковування *.fnt файлів
/// @param inputFile вхідна папка

public void compileFont (File inputFile) {

Graphics g;
CharEntry charEntry;
BufferedImage tmpImage;
BufferedImage fontImage;
int fontWidth = -1, fontHeight = -1;
File[] allFiles = inputFile.listFiles();
ByteArrayOutputStream baos = new ByteArrayOutputStream();

// ............................................................................
// Зчитування заголовка шрифту

if (debug) { out.println(" --- Read Font Header --- "); }

for (File headerFile : allFiles) {
  if (headerFile.getName().endsWith(".bin")) {
    String name = headerFile.getName();
    fontWidth  = Integer.parseInt(name.split("x")[0]);
    fontHeight = Integer.parseInt(name.split("x")[1].split("\\.")[0]);
    try { baos.write(Files.readAllBytes(headerFile.toPath())); }
    catch (IOException e) { showErrorMessage(e);
                            return; } } }

if (debug) { out.println(" --- Done --- "); }

// ............................................................................
// Зчитування усіх зображень у папці

if (debug) { out.println(" --- Read All Chars --- "); }

fontImage = new BufferedImage(fontWidth, fontHeight * 2, TYPE_3BYTE_BGR);
g = fontImage.getGraphics();

for (int z = 0; z < allFiles.length; z++) {

String num = String.format("%03d", z + 1);

for (File file : allFiles) {
  if (file.getName().startsWith(num)) {

    try { String[] nameParts = file.getName().split("_");
      char charC     = Utils.fromStringToChar(nameParts[1]);
      int charX      = Integer.parseInt(nameParts[2]);
      int charW      = Integer.parseInt(nameParts[3]);
      int unknownOne = Integer.parseInt(nameParts[4]);
      int unknownTwo = Integer.parseInt(nameParts[5].split("\\.")[0]);
      tmpImage = ImageIO.read(file);
      g.drawImage(tmpImage, charX, 0, null);
      charEntry = new CharEntry(charC, charX, charW,
                                unknownOne, unknownTwo);
      baos.write(charEntry.toBytes());
      break; }

    catch (IOException | NumberFormatException e) { showErrorMessage(e);
                                                    return; } } } }

if (debug) { out.println(" --- Done --- "); }

// ............................................................................
// Запис налагоджувального зображення у файл

if (debug) 
  { File imgFile = new File(inputFile.getPath() + "_out.bmp");
    try { ImageIO.write(fontImage, "bmp", imgFile); }
    catch (IOException e) { showErrorMessage(e); } }

// ............................................................................
// Запаковування зображень

if (debug) { out.println(" --- Prepare images --- "); }

for (int z = 0; z <= 1; z++) {
    
  tmpImage = fontImage.getSubimage(0, z * fontHeight, fontWidth, fontHeight);

  tmpBuffer = ByteBuffer.allocate(fontWidth * fontHeight * 2);
  tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);

  for (int y = 0; y < fontHeight; y++) {
  for (int x = 0; x < fontWidth;  x++) {
    int rgb888 = tmpImage.getRGB(x, y);
    short rgb565 = Utils.from888to565rgb(rgb888);
    tmpBuffer.putShort(rgb565);
  }
  }

  byte[] imgData = Utils.bzip2Compress(tmpBuffer.array());

  tmpBuffer = ByteBuffer.allocate(12);
  tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);

  tmpBuffer.putShort((short) fontWidth);  // ширина зображення
  tmpBuffer.putShort((short) fontHeight); // висота зображення
  tmpBuffer.putInt(2);                    // тип стиснення - bzip2
  tmpBuffer.putInt(imgData.length);       // розмір стиснених даних
    
  byte[] imgHeader = tmpBuffer.array();

  try { baos.write(imgHeader);
        baos.write(imgData); }
  catch (IOException e) { showErrorMessage(e); }
}

if (debug) { out.println(" --- Done --- "); }

// ............................................................................
// Запис зібраного шрифту в файл

if (debug) { out.println(" --- Write Result --- "); }

File outputFile = new File(inputFile.getPath() + ".fnt");

try (FileOutputStream fos = new FileOutputStream(outputFile))
  { fos.write(baos.toByteArray()); }
catch (Exception e)
  { showErrorMessage(e); }

if (debug) { out.println(" --- Done --- "); }

}

// ============================================================================

private void showErrorMessage (Exception e)
  { showMessageDialog(mainWindow, "Відбулася критична помилка!\n"
                                 + e.getMessage(), "Помилка", 0); }

// Кінець класу FontProcessor =================================================

}