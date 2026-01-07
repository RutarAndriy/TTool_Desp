package com.rutar.ttool_desp;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import javax.swing.*;
import java.util.jar.*;
import java.nio.file.*;
import javax.imageio.*;
import java.awt.image.*;
import java.awt.event.*;
import java.nio.charset.*;
import javax.swing.event.*;
import javax.swing.table.*;
import com.formdev.flatlaf.*;
import javax.swing.filechooser.*;
import com.formdev.flatlaf.themes.*;
import org.apache.commons.compress.compressors.bzip2.*;
//import org.apache.commons.compress.compressors.

import static java.io.File.*;
import static java.lang.System.*;
import static javax.swing.JOptionPane.*;
import static javax.swing.JFileChooser.*;
import static java.awt.image.BufferedImage.*;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;

// ............................................................................
/// Головний клас програми
/// @author Rutar_Andriy
/// 27.12.2025

public class TToolDesp extends JFrame {

private File inputFile;                                         // вхідний файл
// private File outputFile;                                    // вихідний файл

private final JFileChooser fntCompile;                  // компілювання шрифтів
private final JFileChooser fntDecompile;              // декомпілювання шрифтів

private String appDescription;                                 // опис програми
private DefaultTableModel tableModel;              // стандартна модель таблиці

// ............................................................................

private byte[] allBytes;                                   // всі зчитані байти
private ByteBuffer buffer;                        // буфер для зчитування даних

// Домашня директорія користувача
private final File homeDir = FileSystemView.getFileSystemView()
                                           .getHomeDirectory();

// Фільтр для файлів із розширенням *.fnt
private final FileNameExtensionFilter extFnt =
          new FileNameExtensionFilter("Desperados файли шрифтів", "fnt");

private SearchDialog searchDialog;         // діалогове вікно пошуку інформації

public static boolean debug = true;  // якщо true - увімк. режим налагоджування

// ============================================================================
/// Конструктор за замовчуванням

public TToolDesp() {

initComponents();

fntDecompile = new JFileChooser();
fntDecompile.setFileSelectionMode(FILES_ONLY);
fntDecompile.removeChoosableFileFilter(fntDecompile
            .getChoosableFileFilters()[0]);
fntDecompile.addChoosableFileFilter(extFnt);
fntDecompile.setCurrentDirectory(homeDir);

fntCompile = new JFileChooser();
fntCompile.setFileSelectionMode(DIRECTORIES_ONLY);
fntCompile.removeChoosableFileFilter(fntCompile
          .getChoosableFileFilters()[0]);
fntCompile.addChoosableFileFilter(extFnt);
fntCompile.setCurrentDirectory(homeDir);

}

// ============================================================================
/// Головний метод програми
/// @param args масив переданих параметрів

public static void main (String args[]) {
    
    if (args.length > 0 &&
        args[0].equals("--debug")) { debug = true; }
    
    Map<String, String> defaults = new HashMap<>();
    defaults.put("@accentColor", "#FF0000");
    FlatLaf.setGlobalExtraDefaults(defaults);

    UIManager.put("MenuItem.minimumIconSize", new Dimension(0, 0));
    UIManager.put("MenuItem.selectionType", "underline");
    UIManager.put("MenuBar.selectionType", "underline");
    UIManager.put("MenuItem.iconTextGap", 0);

    try { FlatMacDarkLaf.setup(); }
    catch (Exception e) {}
    
    EventQueue.invokeLater(() -> {
        new TToolDesp().setVisible(true);
    });
}

// ============================================================================
/// Відображення інформації про програму

private void showInfoDialog() {

// Отримуємо текст опису програми
if (appDescription == null) {

URL descriptionUrl = getClass().getResource("others/appDescription.txt");
URL channelUrl     = getClass().getResource("others/channelURL.txt");
URL manifestUrl    = getClass().getClassLoader()
                    .getResource("META-INF/MANIFEST.MF");

try (InputStream desc = descriptionUrl.openStream();
     InputStream link = channelUrl    .openStream();
     InputStream data = manifestUrl   .openStream()) {

Attributes attributes = new Manifest(data).getMainAttributes();
    
String channelURL = new String(link.readAllBytes(), StandardCharsets.UTF_8);
String appVersion = attributes.getValue("Version");
String buildDate  = attributes.getValue("Build-Date");

appVersion = (appVersion == null) ? "0.0.1" : appVersion;
buildDate  = (buildDate  == null) ? "25.04.1995" : buildDate.split(" ")[0];

appDescription = new String(desc.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(channelURL, appVersion, buildDate); }

catch (IOException _) {} }

// ............................................................................

JEditorPane pane = new JEditorPane("text/html", appDescription);
pane.setEditable(false);
pane.setFocusable(false);

pane.addHyperlinkListener((HyperlinkEvent e) -> {
    if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
        try { Desktop.getDesktop().browse(e.getURL().toURI()); }
        catch (IOException | URISyntaxException _) { }
    }
});

showMessageDialog(this, pane, "Про програму", INFORMATION_MESSAGE);

}

// ============================================================================
/// Відображення вікна пошуку інформації

private void showSearchDialog() {
        
    if (searchDialog == null) { searchDialog = new SearchDialog(this); }    
    searchDialog.setVisible(true);

}

// ============================================================================
/// Відображення вікна підтвердження виходу

private void showExitDialog() { System.exit(0); }

// ============================================================================
/// Вибір шрифту для розпакування

private void showDecompileFontDialog() {

int result = fntDecompile.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

inputFile = fntDecompile.getSelectedFile();

try { allBytes = Files.readAllBytes(inputFile.toPath()); }

catch (IOException e) { showMessageDialog(this, "Відбулася критична помилка!");
                        return; }

// ............................................................................

byte[] data;
buffer = ByteBuffer.wrap(allBytes);
buffer.order(ByteOrder.LITTLE_ENDIAN);

ByteBuffer fontHeaderBuffer = ByteBuffer.allocate(70);
fontHeaderBuffer.order(ByteOrder.LITTLE_ENDIAN);

// ............................................................................
if (debug) { out.println(" --- Font Header --- "); }

// Сигнатура
data = new byte[6];
buffer.get(data);
fontHeaderBuffer.put(data);
if (debug) { out.println("Signature: " + new String(data)); }

// Доп. змінна
int aInt = buffer.getInt();
fontHeaderBuffer.putInt(aInt);
if (debug) { out.println("Type_1?: " + aInt); }

// Назва шрифту
data = new byte[36];
buffer.get(data);
fontHeaderBuffer.put(data);
if (debug) { out.println("Font name: " + new String(data)); }

// Тип чогось
short aShort = buffer.getShort();
fontHeaderBuffer.putShort(aShort);
if (debug) { out.println("Type_2?: " + aShort); }

// Тип чогось
short a2Short = buffer.getShort();
fontHeaderBuffer.putShort(a2Short);
if (debug) { out.println("Type_3?: " + a2Short); }

// Висота шрифта
int fontHeight = buffer.getInt();
fontHeaderBuffer.putInt(fontHeight);
if (debug) { out.println("FontHeight: " + fontHeight); }

// Ширина симв. квадрату
int leterRectangleWidth = buffer.getInt();
fontHeaderBuffer.putInt(leterRectangleWidth);
if (debug) { out.println("RectangleWidth: " + leterRectangleWidth); }

// Макс. ширина символу
int maxLetterWidth = buffer.getInt();
fontHeaderBuffer.putInt(maxLetterWidth);
if (debug) { out.println("MaxLetterWidth: " + maxLetterWidth); }

// Кількість символів у шрифті
int entryCount = buffer.getInt();
fontHeaderBuffer.putInt(entryCount);
if (debug) { out.println("EntryCount: " + entryCount); }

// Перевірка заданої умови
if (aInt >= 512) {
    // Невідома змінна
    int a2Int = buffer.getInt();
    fontHeaderBuffer.putInt(a2Int);
    if (debug) { out.println("Type_4?: " + a2Int); }
}

// ............................................................................
if (debug) { out.println(" --- Chars --- "); }

ArrayList<CharEntry> charEntries = new ArrayList<>();

for (int z = 0; z < entryCount; z++) {
    
    String num = String.format("%03d", z + 1);
    CharEntry entry = new CharEntry(buffer.getChar(),
                                    buffer.getInt(), buffer.getInt(),
                                    buffer.getInt(), buffer.getInt());
    
    charEntries.add(entry);
    
    if (debug) { out.println(num + " - " + entry.toString()); }
}

// ............................................................................
if (debug) { out.println(" --- Images --- "); }

// Створення вихідної папки
File outDir = new File(inputFile.getParent() + separator +
                       inputFile.getName().split("\\.")[0]);
outDir.mkdir();

// Отримання сирих батів заголовку шрифта
fontHeaderBuffer.flip();
byte[] fontHeader = new byte[fontHeaderBuffer.remaining()];
fontHeaderBuffer.get(fontHeader);

File headerFile = new File(outDir.getPath() + separator + "fontHeader.bin");

// Запис заголовку шрифта у файл
try (FileOutputStream fis = new FileOutputStream(headerFile))
    { fis.write(fontHeader); }
catch (Exception _) {}

// ............................................................................

for (int z = 1; z <= 2; z++) {

int color;
short width  = buffer.getShort(); // uint16_t width
short height = buffer.getShort(); // uint16_t height
int compType = buffer.getInt();   // uint32_t compression_type
int compSize = buffer.getInt();   // uint32_t size_compressed

data = new byte[compSize];
buffer.get(data); // uint8_t[size_compressed] compressed_data

// розпаковуємо стиснені дані
data = compType == 1 ? zlibDecompress(data) : bzip2Decompress(data);

if (debug) { out.println("Image_" + z + ", " + width + "x" + height
                       + ", comp = " + compType
                       + ", size = " + compSize); }

BufferedImage image = new BufferedImage(width, height, TYPE_3BYTE_BGR);

// Зчитуємо розпаковані дані та відтворюємо зображення зі шрифтом
ByteBuffer imageBuffer = ByteBuffer.wrap(data);
imageBuffer.order(ByteOrder.LITTLE_ENDIAN);

for (int r = 0; r < height; r++) {
for (int c = 0; c < width; c++) {
    color = Utils.from565to888rgb(imageBuffer.getShort());
    image.setRGB(c, r, color);
}
}

// Проходимося по всіх символах та записуємо їх у файли
for (int q = 0; q < charEntries.size(); q++) {
    
    CharEntry entry = charEntries.get(q);
    String num = String.format("%03d", q + 1);
    
    int imW = leterRectangleWidth + 1;
    int imH = fontHeight;
    int x = entry.getCharX();

    // Обробка можливого виходу за фактичні розміри зображення
    if (x + imW > image.getWidth()) { imW = image.getWidth() - x; }

    // Отримання частинки зображення із конкретним символом
    BufferedImage subimage = image.getSubimage(x, 0, imW, imH);
    
    String code = Utils.fromCharToString(entry.getChar());
    
    String name = "x" + z                      // порядковий номер зображення
                + "_" + num                    // порядковий номер символу
                + "_" + code                   // код символу
                + "_" + entry.getCharX()       // горизонтальний зсув символу
                + "_" + entry.getCharW()       // ширина символу
                + "_" + entry.getUnknownOne()  // невід. параметр 1
                + "_" + entry.getUnknownTwo(); // невід. параметр 2
    
    File entryFile = new File(outDir.getPath() + separator + name + ".bmp");

    try { ImageIO.write(subimage, "bmp", entryFile); }
    catch (IOException e) { err.println(e.getMessage());
                            return; }
}
}

if (debug) { System.out.println(" --- xxx --- "); }

showMessageDialog(this, "Шрифт успішно розібрано!");

}

// ============================================================================
/// Вибір розпакованого шрифту для пакування

private void showCompileFontDialog() {

int result = fntCompile.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

inputFile = fntCompile.getSelectedFile();
File[] allFiles = inputFile.listFiles();

File file;
String fileName;
int maxImageWidth = 0;
int entryCount = (allFiles.length - 1) / 2;
ArrayList<BufferedImage> images = new ArrayList<>();
ArrayList<CharEntry> charEntries = new ArrayList<>();

// ............................................................................
// Зчитування усіх зображень у папці

for (int z = 1; z <= 2; z++) {

for (int q = 0; q < entryCount; q++) {

String num = String.format("%03d", q + 1);

for (int f = 0; f < allFiles.length; f++) {
    
    file = allFiles[f];
    fileName = file.getName();
    
    if (fileName.startsWith("x" + z + "_" + num)) { 
            
        try { String[] nameParts = fileName.split("_");
              char charC     = Utils.fromStringToChar(nameParts[2]);
              int charX      = Integer.parseInt(nameParts[3]);
              int charW      = Integer.parseInt(nameParts[4]);
              int unknownOne = Integer.parseInt(nameParts[5]);
              int unknownTwo = Integer.parseInt(nameParts[6].split("\\.")[0]);
              charEntries.add(new CharEntry(charC, charX, charW,
                                            unknownOne, unknownTwo));
          
              if (charX + charW > maxImageWidth) { maxImageWidth = charX +
                                                                   charW; }
          
              images.add(ImageIO.read(file));
              f = allFiles.length; }
        
        catch (IOException | NumberFormatException e)
            { showMessageDialog(this, "Відбулася критична помилка\n"
                                     + e.getMessage(), "Помилка", 0);
              return; } } } } }

// ............................................................................

showMessageDialog(this, "Шрифт успішно зібрано!");

}

// ============================================================================
/// Розпакування даних, запакованих з допомогою алгоритму zlib

private byte[] zlibDecompress (byte[] compressed) {

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
/// Розпакування даних, запакованих з допомогою алгоритму bzip2

private byte[] bzip2Decompress (byte[] compressed) {

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

// ============================================================================
/// Цей метод викликається з конструктора для ініціалізації форми.
/// УВАГА: НЕ змінюйте цей код. Вміст цього методу завжди 
/// перезапишеться редактором форм

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sp_table = new JScrollPane();
        tbl_main = new JTable();
        pnl_footer = new JPanel();
        lbl_colCount = new JLabel();
        lbl_rowCount = new JLabel();
        mnb_main = new JMenuBar();
        mn_file = new JMenu();
        mni_open = new JMenuItem();
        mni_save = new JMenuItem();
        sep_one = new JPopupMenu.Separator();
        mni_find = new JMenuItem();
        sep_two = new JPopupMenu.Separator();
        mni_exit = new JMenuItem();
        mn_edit = new JMenu();
        mni_fntDecompile = new JMenuItem();
        mni_fntCompile = new JMenuItem();
        mn_info = new JMenu();
        mni_about = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("TTool_Desp");
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                onWindowClose(evt);
            }
        });

        tbl_main.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tbl_main.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tbl_main.setAutoscrolls(false);
        tbl_main.setIntercellSpacing(new Dimension(2, 2));
        tbl_main.setRowSelectionAllowed(false);
        tbl_main.setShowGrid(true);
        tbl_main.getTableHeader().setReorderingAllowed(false);
        sp_table.setViewportView(tbl_main);

        pnl_footer.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 5));

        lbl_colCount.setText("Кількість стовбців: 0");
        pnl_footer.add(lbl_colCount);

        lbl_rowCount.setText("Кількість рядків: 0");
        pnl_footer.add(lbl_rowCount);

        mn_file.setText("Файл");

        mni_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        mni_open.setText("Відкрити");
        mni_open.setActionCommand("open");
        mni_open.setEnabled(false);
        mni_open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_file.add(mni_open);

        mni_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        mni_save.setText("Зберегти");
        mni_save.setActionCommand("save");
        mni_save.setEnabled(false);
        mni_save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_file.add(mni_save);
        mn_file.add(sep_one);

        mni_find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        mni_find.setText("Пошук");
        mni_find.setActionCommand("find");
        mni_find.setEnabled(false);
        mni_find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_file.add(mni_find);
        mn_file.add(sep_two);

        mni_exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        mni_exit.setText("Вихід");
        mni_exit.setActionCommand("exit");
        mni_exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_file.add(mni_exit);

        mnb_main.add(mn_file);

        mn_edit.setText("Правка");

        mni_fntDecompile.setText("Розпакувати шрифт");
        mni_fntDecompile.setActionCommand("decompileFont");
        mni_fntDecompile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_fntDecompile);

        mni_fntCompile.setText("Запакувати шрифт");
        mni_fntCompile.setActionCommand("compileFont");
        mni_fntCompile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_fntCompile);

        mnb_main.add(mn_edit);

        mn_info.setText("Інфо");

        mni_about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
        mni_about.setText("Про програму");
        mni_about.setActionCommand("info");
        mni_about.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_info.add(mni_about);

        mnb_main.add(mn_info);

        setJMenuBar(mnb_main);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(sp_table, GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                    .addComponent(pnl_footer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sp_table, GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_footer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

// ============================================================================
/// Прослуховування пунктів меню програми

    private void onMenuClick(ActionEvent evt) {//GEN-FIRST:event_onMenuClick

    switch (evt.getActionCommand()) {
        
        case "find" -> showSearchDialog();
        case "exit" -> showExitDialog();
        case "info" -> showInfoDialog();
        
        case "decompileFont" -> showDecompileFontDialog();
        case "compileFont"   -> showCompileFontDialog();
        
    }   
    }//GEN-LAST:event_onMenuClick

// ============================================================================
/// Прослуховування закривання вікна

    private void onWindowClose(WindowEvent evt) {//GEN-FIRST:event_onWindowClose
        showExitDialog();
    }//GEN-LAST:event_onWindowClose

// ============================================================================
/// Список усіх об'явлених змінних

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel lbl_colCount;
    private JLabel lbl_rowCount;
    private JMenu mn_edit;
    private JMenu mn_file;
    private JMenu mn_info;
    private JMenuBar mnb_main;
    private JMenuItem mni_about;
    private JMenuItem mni_exit;
    private JMenuItem mni_find;
    private JMenuItem mni_fntCompile;
    private JMenuItem mni_fntDecompile;
    private JMenuItem mni_open;
    private JMenuItem mni_save;
    private JPanel pnl_footer;
    private JPopupMenu.Separator sep_one;
    private JPopupMenu.Separator sep_two;
    private JScrollPane sp_table;
    public JTable tbl_main;
    // End of variables declaration//GEN-END:variables

// Кінець класу TToolDesp =====================================================

}
