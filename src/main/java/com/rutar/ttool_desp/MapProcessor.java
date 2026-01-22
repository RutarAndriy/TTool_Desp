package com.rutar.ttool_desp;

import java.io.*;
import java.awt.*;
import java.nio.*;
import javax.imageio.*;
import java.awt.image.*;

import static java.lang.System.*;
import static javax.swing.JOptionPane.*;
import static java.awt.image.BufferedImage.*;
import static com.rutar.ttool_desp.TToolDesp.*;

// ............................................................................
/// Компіляція та декомпіляція *.map файлів
/// @author Rutar_Andriy
/// 22.01.2026

public class MapProcessor {

private byte[] data;
private File tmpFile;
private ByteBuffer mainBuffer, tmpBuffer;

private final TToolDesp mainWindow;

// ============================================================================
/// Конструктор за замовчуванням
/// @param mainWindow головне вікно програми

public MapProcessor (TToolDesp mainWindow) { this.mainWindow = mainWindow; }

// ============================================================================
/// Розпаковування *.map файлів
/// @param allBytes усі зчитані байти файлу
/// @param outputFile вихідний файл

public void decompileMap (byte[] allBytes, File outputFile) {

int color;
BufferedImage mapImage;                                     // зображення карти

mainBuffer = ByteBuffer.wrap(allBytes);
mainBuffer.order(ByteOrder.LITTLE_ENDIAN);

tmpBuffer = ByteBuffer.allocate(70);
tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);

// ............................................................................
if (debug) { out.println(" --- Map Header --- "); }

short mapW = mainBuffer.getShort();                             // ширина карти
if (debug) { out.println("Map width: " + mapW); }

short mapH = mainBuffer.getShort();                             // висота карти
if (debug) { out.println("Map height: " + mapH); }

int compType = mainBuffer.getInt();                      // тип стиснення даних 
if (debug) { out.println("Compressed type: " + compType); }

int compSize = mainBuffer.getInt();                   // розмір стиснення даних 
if (debug) { out.println("Compressed size: " + compSize); }

// Отримання стиснених байтів карти
data = new byte[compSize];
mainBuffer.get(data);

// Розпаковування стиснених даних
data = compType == 1 ? Utils.zlibDecompress(data) :
                       Utils.bzip2Decompress(data);

// ............................................................................

if (debug) { out.println("Map, " + mapW + "x" + mapH
                       + ", comp = " + compType + ", size = " + compSize); }

mapImage = new BufferedImage(mapW, mapH, TYPE_3BYTE_BGR);

// ............................................................................
// Зчитування розпакованих даних та відтворення зображення

tmpBuffer = ByteBuffer.wrap(data);
tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);

for (int y = 0; y < mapH; y++) {
for (int x = 0; x < mapW; x++) {
    color = Utils.from565to888rgb(tmpBuffer.getShort());
    mapImage.setRGB(x, y, color);
}
}

// ............................................................................
// Запис розпакованої карти у *.bmp файл

try { ImageIO.write(mapImage, "bmp", outputFile); }
catch (IOException e) { showErrorMessage(e);
                        return; }

if (debug) { out.println(" --- xxx --- "); }

}

// ============================================================================
/// Запапаковування *.map файлів
/// @param inputFile вхідний *.bmp файл

public void compileMap (File inputFile) {

try {

if (debug) { out.println(" --- Read Image --- "); }

// Читаємо файл зображення
BufferedImage imgMap = ImageIO.read(inputFile);
Graphics g = imgMap.getGraphics();

int mapW = imgMap.getWidth();
int mapH = imgMap.getHeight();

tmpBuffer = ByteBuffer.allocate(mapW * mapH * 2);
tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);

// Перетворюємо кольори пікселів у формат rgb565
for (int y = 0; y < mapH; y++) {
for (int x = 0; x < mapW;  x++) {
    int rgb888 = imgMap.getRGB(x, y);
    short rgb565 = Utils.from888to565rgb(rgb888);
    tmpBuffer.putShort(rgb565);
}
}

// Стискаємо дані
byte[] imgData = Utils.bzip2Compress(tmpBuffer.array());

int compType = 2;                                      // тип стиснення - bzip2
int compSize = imgData.length;                        // розмір стиснених даних

// ............................................................................
// Записуємо заголовок *.map файлу

tmpBuffer = ByteBuffer.allocate(12);
tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);

tmpBuffer.putShort((short) mapW);                               // ширина карти
tmpBuffer.putShort((short) mapH);                               // висота карти
tmpBuffer.putInt(compType);                            // тип стиснення - bzip2
tmpBuffer.putInt(compSize);                           // розмір стиснених даних

byte[] imgHeader = tmpBuffer.array();

// ............................................................................

ByteArrayOutputStream baos = new ByteArrayOutputStream();
baos.write(imgHeader);
baos.write(imgData);

if (debug) { out.println(" --- Write Map --- "); }

// ............................................................................
// Записуємо результат у файл

File outputFile = new File(inputFile.getPath().replace(".bmp", ".map"));

try (FileOutputStream fos = new FileOutputStream(outputFile))
    { fos.write(baos.toByteArray()); }
catch (Exception e)
    { showErrorMessage(e); }

if (debug) { out.println(" --- Done --- "); }

}

catch (IOException e) { showErrorMessage(e); }

}

// ============================================================================

private void showErrorMessage (Exception e) {
    
    showMessageDialog(mainWindow, "Відбулася критична помилка!\n"
                                 + e.getMessage(), "Помилка", 0);
}

// Кінець класу MapProcessor ==================================================

}