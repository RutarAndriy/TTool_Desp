package com.rutar.ttool_desp;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.*;
import java.util.jar.*;
import java.nio.file.*;
import java.awt.image.*;
import java.awt.event.*;
import java.nio.charset.*;
import javax.swing.event.*;
import javax.swing.table.*;
import com.formdev.flatlaf.*;
import javax.swing.filechooser.*;
import com.rutar.ua_translator.*;
import com.formdev.flatlaf.themes.*;

import static java.io.File.*;
import static javax.swing.JOptionPane.*;
import static javax.swing.JFileChooser.*;

// ............................................................................
/// Головний клас програми
/// @author Rutar_Andriy
/// 27.12.2025

public class TToolDesp extends JFrame {

private File inputFile;                                         // вхідний файл
private File outputFile;                                       // вихідний файл

private final JFileChooser fntCompile;                  // компілювання шрифтів
private final JFileChooser fntDecompile;              // декомпілювання шрифтів
private final JFileChooser mapCompile;                     // компілювання карт
private final JFileChooser mapDecompile;                 // декомпілювання карт
private final JFileChooser sxtCompile;       // компілювання gameover-логотипів
private final JFileChooser sxtDecompile;   // декомпілювання gameover-логотипів

private final RawProcessor rawProcessor;                       // обробник карт
private final FontProcessor fontProcessor;                  // обробник шрифтів

private String appDescription;                                 // опис програми

// ............................................................................

private File tmp;                                           // допоміжна змінна
private byte[] allBytes;                                   // всі зчитані байти
private ByteBuffer buffer;                        // буфер для зчитування даних
private SearchDialog searchDialog;         // діалогове вікно пошуку інформації

// Домашня директорія користувача
public static final File HOME_DIR = FileSystemView.getFileSystemView()
                                                  .getHomeDirectory();

public static boolean debug = false; // якщо true - увімк. режим налагоджування

// ============================================================================
/// Конструктор за замовчуванням

public TToolDesp() {

initComponents();
initAppIcons();

rawProcessor  = new RawProcessor(this);
fontProcessor = new FontProcessor(this);

fntDecompile = Utils.getFileChooser("fnt", FILES_ONLY,
                                    "Desperados файли шрифтів");
fntCompile   = Utils.getFileChooser("fnt", DIRECTORIES_ONLY,
                                    "Desperados файли шрифтів");
mapDecompile = Utils.getFileChooser("map", FILES_ONLY,
                                    "Desperados запаковані карти");
mapCompile   = Utils.getFileChooser("bmp", FILES_ONLY,
                                    "Desperados розпаковані карти");
sxtDecompile = Utils.getFileChooser("sxt", FILES_ONLY,
                                    "Desperados запаковані логотипи");
sxtCompile   = Utils.getFileChooser("bmp", FILES_ONLY,
                                    "Desperados розпаковані логотипи");

}

// ============================================================================
/// Головний метод програми
/// @param args масив переданих параметрів

public static void main (String args[]) {
    
    if (args.length > 0 &&
        args[0].equals("--debug")) { debug = true; }
    
    // ........................................................................
    
    UATranslator.init();
    UIManager.put("FileChooser.readOnly", true);

    JFrame .setDefaultLookAndFeelDecorated(true);
    JDialog.setDefaultLookAndFeelDecorated(true);
    
    FlatLaf.registerCustomDefaultsSource("com.rutar.ttool_desp.themes");

    try { FlatMacDarkLaf.setup(); }
    catch (Exception e) {}
    
    // ........................................................................
    
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

// Читання всіх байтів шрифту
try { allBytes = Files.readAllBytes(inputFile.toPath()); }
catch (IOException e)
    { showMessageDialog(this, "Відбулася критична помилка!", "Помилка", 0);
      return; }

// Створення вихідної папки
outputFile = new File(inputFile.getParent() + separator +
                      inputFile.getName().split("\\.")[0]);

fontProcessor.decompileFont(allBytes, outputFile);

// Відображення інформаційного повідомення
showMessageDialog(this, "Шрифт успішно розібрано!");

}

// ============================================================================
/// Вибір розпакованого шрифту для пакування

private void showCompileFontDialog() {

tmp = Utils.getLastDir(fntDecompile);
if (tmp != null) { fntCompile.setCurrentDirectory(tmp); }

int result = fntCompile.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

inputFile = fntCompile.getSelectedFile();
fontProcessor.compileFont(inputFile);

showMessageDialog(this, "Шрифт успішно зібрано!");

}

// ============================================================================
/// Вибір запакованої карти для розпакування

private void showDecompileMapDialog() {

int result = mapDecompile.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

inputFile = mapDecompile.getSelectedFile();

// Читання всіх байтів карти
try { allBytes = Files.readAllBytes(inputFile.toPath()); }
catch (IOException e)
    { showMessageDialog(this, "Відбулася критична помилка!", "Помилка", 0);
      return; }

// Створення вихідного файлу
outputFile = new File(inputFile.getPath().replace(".map", ".bmp"));

rawProcessor.decompileRaw(allBytes, outputFile);

// Відображення інформаційного повідомення
showMessageDialog(this, "Карту успішно розпаковано!");

}

// ============================================================================
/// Вибір розпакованої карти для запакування

private void showCompileMapDialog() {

tmp = Utils.getLastDir(mapDecompile);
if (tmp != null) { mapCompile.setCurrentDirectory(tmp); }

int result = mapCompile.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

inputFile = mapCompile.getSelectedFile();
rawProcessor.compileRaw(inputFile, "map");

showMessageDialog(this, "Карту успішно запаковано!");

}

// ============================================================================
/// Вибір запакованого gameover-логотипу для розпакування

private void showDecompileSxtDialog() {

int result = sxtDecompile.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

inputFile = sxtDecompile.getSelectedFile();

// Читання всіх байтів карти
try { allBytes = Files.readAllBytes(inputFile.toPath()); }
catch (IOException e)
    { showMessageDialog(this, "Відбулася критична помилка!", "Помилка", 0);
      return; }

// Створення вихідного файлу
outputFile = new File(inputFile.getPath().replace(".sxt", ".bmp"));

rawProcessor.decompileRaw(allBytes, outputFile);

// Відображення інформаційного повідомення
showMessageDialog(this, "Логотип успішно розпаковано!");

}

// ============================================================================
/// Вибір розпакованого gameover-логотипу для запакування

private void showCompileSxtDialog() {

tmp = Utils.getLastDir(sxtDecompile);
if (tmp != null) { sxtCompile.setCurrentDirectory(tmp); }

int result = sxtCompile.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

inputFile = sxtCompile.getSelectedFile();
rawProcessor.compileRaw(inputFile, "sxt");

showMessageDialog(this, "Логотип успішно запаковано!");

}

// ============================================================================
/// Встановлення іконок для головного вікна

private void initAppIcons() {

    BufferedImage icon;
    ArrayList<Image> appIcons = new ArrayList<>();

    try {
        
    for (String resource : new String[] { "icon_16.png",
                                          "icon_32.png" }) {
        resource = "icons/" + resource;
        icon = ImageIO.read(getClass().getResourceAsStream(resource));
        appIcons.add(icon); }
    
    setIconImages(appIcons); }
    
    catch (IOException _) { }
    
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
        sep_three = new JPopupMenu.Separator();
        mni_mapDecompile = new JMenuItem();
        mni_mapCompile = new JMenuItem();
        sep_four = new JPopupMenu.Separator();
        mni_sxtDecompile = new JMenuItem();
        mni_sxtCompile = new JMenuItem();
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
        mn_edit.add(sep_three);

        mni_mapDecompile.setText("Розпакувати *.map");
        mni_mapDecompile.setActionCommand("decompileMap");
        mni_mapDecompile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_mapDecompile);

        mni_mapCompile.setText("Запакувати *.map");
        mni_mapCompile.setActionCommand("compileMap");
        mni_mapCompile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_mapCompile);
        mn_edit.add(sep_four);

        mni_sxtDecompile.setText("Розпакувати *.sxt");
        mni_sxtDecompile.setActionCommand("decompileSxt");
        mni_sxtDecompile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_sxtDecompile);

        mni_sxtCompile.setText("Запакувати *.sxt");
        mni_sxtCompile.setActionCommand("compileSxt");
        mni_sxtCompile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_sxtCompile);

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
        
        case "decompileMap"  -> showDecompileMapDialog();
        case "compileMap"    -> showCompileMapDialog();
        
        case "decompileSxt"  -> showDecompileSxtDialog();
        case "compileSxt"    -> showCompileSxtDialog();
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
    private JMenuItem mni_mapCompile;
    private JMenuItem mni_mapDecompile;
    private JMenuItem mni_open;
    private JMenuItem mni_save;
    private JMenuItem mni_sxtCompile;
    private JMenuItem mni_sxtDecompile;
    private JPanel pnl_footer;
    private JPopupMenu.Separator sep_four;
    private JPopupMenu.Separator sep_one;
    private JPopupMenu.Separator sep_three;
    private JPopupMenu.Separator sep_two;
    private JScrollPane sp_table;
    public JTable tbl_main;
    // End of variables declaration//GEN-END:variables

// Кінець класу TToolDesp =====================================================

}
