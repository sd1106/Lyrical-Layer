import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class PWAApp extends JFrame {

    private static final Color TAYLOR_BG_MAIN  = new Color(255, 240, 245);
    private static final Color TAYLOR_BG_CARD  = new Color(255, 220, 230);
    private static final Color TAYLOR_ACCENT   = new Color(190,  50,  90);
    private static final Color TAYLOR_ACCENT2  = new Color(220, 100, 130);
    private static final Color TAYLOR_TEXT     = new Color( 40,  10,  20);
    private static final Color TAYLOR_SUBTEXT  = new Color(110,  50,  70);
    private static final Color TAYLOR_BORDER   = new Color(210, 160, 175);

    private static final Color KENDRICK_BG_MAIN = new Color( 14,  12,   8);
    private static final Color KENDRICK_BG_CARD = new Color( 28,  24,  14);
    private static final Color KENDRICK_ACCENT  = new Color(212, 175,  55);
    private static final Color KENDRICK_ACCENT2 = new Color(170, 130,  30);
    private static final Color KENDRICK_TEXT    = new Color(245, 235, 200);
    private static final Color KENDRICK_SUBTEXT = new Color(160, 140,  90);
    private static final Color KENDRICK_BORDER  = new Color( 80,  65,  20);

    private static final Color HIT_GREEN  = new Color( 80, 200,  80);
    private static final Color MISS_RED   = new Color(220,  80,  80);

    private Color bgMain   = TAYLOR_BG_MAIN;
    private Color bgCard   = TAYLOR_BG_CARD;
    private Color accent   = TAYLOR_ACCENT;
    private Color accent2  = TAYLOR_ACCENT2;
    private Color textMain = TAYLOR_TEXT;
    private Color textSub  = TAYLOR_SUBTEXT;
    private Color borderC  = TAYLOR_BORDER;

    private final Trie taylorTrie   = new Trie();
    private final Trie kendrickTrie = new Trie();
    private Trie activeTrie = taylorTrie;

    private String guessAnswer = null;

    private JTextPane     inputPane;
    private JLabel        nextCharLabel, nextWordLabel;
    private JPanel        top5Panel;
    private JLabel        statusLabel;
    private JToggleButton taylorBtn, kendrickBtn;
    private JPanel        linePanel;
    private JScrollPane   lineScroll;
    private JTextField    guessInput;
    private JLabel        guessFeedback;

    private JPanel        headerPanel, predPanel, guessPanel;
    private JSplitPane    splitPane;
    private JScrollPane   inputScroll, predScroll;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PWAApp().setVisible(true));
    }

    public PWAApp() {
        setTitle("Predictive Writing Assistant  -  Taylor vs Kendrick");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 720);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(bgMain);
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildCenter(),     BorderLayout.CENTER);
        add(buildGuessPanel(), BorderLayout.SOUTH);

        loadLyricsAsync();
    }

    private void applyTheme(boolean isTaylor) {
        bgMain   = isTaylor ? TAYLOR_BG_MAIN   : KENDRICK_BG_MAIN;
        bgCard   = isTaylor ? TAYLOR_BG_CARD   : KENDRICK_BG_CARD;
        accent   = isTaylor ? TAYLOR_ACCENT    : KENDRICK_ACCENT;
        accent2  = isTaylor ? TAYLOR_ACCENT2   : KENDRICK_ACCENT2;
        textMain = isTaylor ? TAYLOR_TEXT      : KENDRICK_TEXT;
        textSub  = isTaylor ? TAYLOR_SUBTEXT   : KENDRICK_SUBTEXT;
        borderC  = isTaylor ? TAYLOR_BORDER    : KENDRICK_BORDER;

        getContentPane().setBackground(bgMain);
        headerPanel.setBackground(bgMain);
        statusLabel.setForeground(textSub);

        if (isTaylor) {
            taylorBtn.setBackground(TAYLOR_ACCENT);
            taylorBtn.setForeground(Color.WHITE);
            kendrickBtn.setBackground(new Color(80, 80, 80));
            kendrickBtn.setForeground(new Color(180, 180, 180));
        } else {
            kendrickBtn.setBackground(KENDRICK_ACCENT);
            kendrickBtn.setForeground(new Color(20, 16, 8));
            taylorBtn.setBackground(new Color(80, 80, 80));
            taylorBtn.setForeground(new Color(180, 180, 180));
        }

        inputPane.setBackground(bgCard);
        inputPane.setForeground(textMain);
        inputPane.setCaretColor(textMain);
        inputScroll.getViewport().setBackground(bgCard);
        inputScroll.setBorder(titledBorder("Type here  (green = in dataset, red = not found)"));

        predPanel.setBackground(bgMain);
        predScroll.getViewport().setBackground(bgMain);
        predScroll.setBorder(BorderFactory.createEmptyBorder());
        nextCharLabel.setForeground(textMain);
        nextWordLabel.setForeground(textMain);
        top5Panel.setBackground(bgMain);

        guessPanel.setBackground(bgCard);
        guessPanel.setBorder(titledBorder("Guessing Game - fill in the blank!"));
        linePanel.setBackground(bgCard);
        lineScroll.getViewport().setBackground(bgCard);
        guessInput.setBackground(bgMain);
        guessInput.setForeground(textMain);
        guessInput.setCaretColor(textMain);
        guessInput.setBorder(BorderFactory.createLineBorder(borderC, 1));
        guessFeedback.setForeground(textSub);

        splitPane.setBackground(bgMain);

        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    private void loadLyricsAsync() {
        statusLabel.setText("Loading lyrics…");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                loadFile(taylorTrie,   "taylor.txt");
                loadFile(kendrickTrie, "kendrick.txt");
                return null;
            }
            @Override protected void done() {
                statusLabel.setText("Lyrics loaded   |   Switch artists to compare predictions");
                refreshPredictions(getLastWord(inputPane.getText()));
            }
        }.execute();
    }

    private void loadFile(Trie trie, String path) {
        try {
            for (String raw : Files.readAllLines(Path.of(path))) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("[") || line.matches("[A-Z\\s]{4,}"))
                    continue;
                String cleaned = line.replaceAll("[^a-zA-Z'\\s]", "").toLowerCase().trim();
                if (!cleaned.isEmpty())
                    trie.insertLine(cleaned.split("\\s+"));
            }
        } catch (IOException e) {
            System.err.println("Could not load " + path + ": " + e.getMessage());
        }
    }

    private JPanel buildHeader() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(bgMain);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 6, 18));

        JLabel title = new JLabel("Predictive Writing Assistant");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(accent);

        taylorBtn   = artistToggle("Taylor Swift",   TAYLOR_ACCENT,        Color.WHITE);
        kendrickBtn = artistToggle("Kendrick Lamar", new Color(80, 80, 80), new Color(180, 180, 180));

        ButtonGroup group = new ButtonGroup();
        group.add(taylorBtn);
        group.add(kendrickBtn);
        taylorBtn.setSelected(true);
        activeTrie = taylorTrie;

        taylorBtn.addActionListener(e -> {
            activeTrie = taylorTrie;
            applyTheme(true);
            rehighlightAll();
            refreshPredictions(getLastWord(inputPane.getText()));
        });
        kendrickBtn.addActionListener(e -> {
            activeTrie = kendrickTrie;
            applyTheme(false);
            rehighlightAll();
            refreshPredictions(getLastWord(inputPane.getText()));
        });

        statusLabel = new JLabel("Loading…");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(textSub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(taylorBtn);
        right.add(kendrickBtn);

        headerPanel.add(title,       BorderLayout.WEST);
        headerPanel.add(right,       BorderLayout.EAST);
        headerPanel.add(statusLabel, BorderLayout.SOUTH);
        return headerPanel;
    }

    private JSplitPane buildCenter() {
        inputPane = new JTextPane();
        inputPane.setBackground(bgCard);
        inputPane.setForeground(textMain);
        inputPane.setCaretColor(textMain);
        inputPane.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        inputPane.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        inputScroll = new JScrollPane(inputPane);
        inputScroll.setBorder(titledBorder("Type here  (green = in dataset, red = not found)"));
        inputScroll.getViewport().setBackground(bgCard);

        inputPane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onTextChanged(); }
            public void removeUpdate(DocumentEvent e)  { onTextChanged(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        nextCharLabel = predLabel("Next letter:  -");
        nextWordLabel = predLabel("Next word:    -");

        top5Panel = new JPanel();
        top5Panel.setLayout(new BoxLayout(top5Panel, BoxLayout.Y_AXIS));
        top5Panel.setBackground(bgMain);
        top5Panel.setBorder(titledBorder("Top 5 completions"));

        predPanel = new JPanel();
        predPanel.setLayout(new BoxLayout(predPanel, BoxLayout.Y_AXIS));
        predPanel.setBackground(bgMain);
        predPanel.setBorder(titledBorder("Predictions"));
        predPanel.add(Box.createVerticalStrut(10));
        predPanel.add(nextCharLabel);
        predPanel.add(Box.createVerticalStrut(8));
        predPanel.add(nextWordLabel);
        predPanel.add(Box.createVerticalStrut(12));
        predPanel.add(top5Panel);
        predPanel.add(Box.createVerticalGlue());

        predScroll = new JScrollPane(predPanel);
        predScroll.getViewport().setBackground(bgMain);
        predScroll.setBorder(BorderFactory.createEmptyBorder());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputScroll, predScroll);
        splitPane.setDividerLocation(560);
        splitPane.setBackground(bgMain);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        return splitPane;
    }

    private JPanel buildGuessPanel() {
        guessPanel = new JPanel(new BorderLayout(8, 4));
        guessPanel.setBackground(bgCard);
        guessPanel.setBorder(titledBorder("Guessing Game - fill in the blank!"));
        guessPanel.setPreferredSize(new Dimension(1080, 120));

        linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        linePanel.setBackground(bgCard);
        lineScroll = new JScrollPane(linePanel);
        lineScroll.setPreferredSize(new Dimension(620, 55));
        lineScroll.getViewport().setBackground(bgCard);
        lineScroll.setBorder(null);

        guessInput = new JTextField(18);
        guessInput.setBackground(bgMain);
        guessInput.setForeground(textMain);
        guessInput.setCaretColor(textMain);
        guessInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        guessInput.setBorder(BorderFactory.createLineBorder(borderC, 1));

        JButton submitBtn  = styledButton("Submit",   Color.WHITE, accent);
        JButton newGameBtn = styledButton("New Line", Color.WHITE, new Color(80, 80, 80));

        guessFeedback = new JLabel("  Press 'New Line' to start!");
        guessFeedback.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        guessFeedback.setForeground(textSub);

        submitBtn.addActionListener(e  -> onGuessSubmit());
        guessInput.addActionListener(e -> onGuessSubmit());
        newGameBtn.addActionListener(e -> onNewGame());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        JLabel guessLbl = new JLabel("Your guess:");
        guessLbl.setForeground(textSub);
        guessLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        controls.add(guessLbl);
        controls.add(guessInput);
        controls.add(submitBtn);
        controls.add(newGameBtn);
        controls.add(guessFeedback);

        guessPanel.add(lineScroll, BorderLayout.CENTER);
        guessPanel.add(controls,   BorderLayout.SOUTH);
        return guessPanel;
    }

    private void onNewGame() {
        String[] result = activeTrie.guessingGame();
        if (result == null || result.length == 0) {
            guessFeedback.setText("  No lyrics loaded yet!");
            return;
        }
        guessAnswer = result[result.length - 1];

        linePanel.removeAll();
        for (int i = 0; i < result.length - 1; i++) {
            JLabel lbl = new JLabel(result[i]);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            lbl.setForeground("___".equals(result[i]) ? accent : textMain);
            linePanel.add(lbl);
        }
        linePanel.revalidate();
        linePanel.repaint();
        lineScroll.revalidate();

        guessInput.setText("");
        guessFeedback.setText("  Fill in the blank  ___");
        guessFeedback.setForeground(textSub);
        guessInput.requestFocus();
    }

    private void onGuessSubmit() {
        if (guessAnswer == null) { guessFeedback.setText("  Press 'New Line' first!"); return; }

        if (guessInput.getText().trim().equalsIgnoreCase(guessAnswer)) {
            guessFeedback.setText("Correct!  The word was: " + guessAnswer);
            guessFeedback.setForeground(HIT_GREEN);
        } else {
            guessFeedback.setText("Nope!  The answer was: " + guessAnswer);
            guessFeedback.setForeground(MISS_RED);
        }
        for (Component c : linePanel.getComponents())
            if (c instanceof JLabel lbl && "___".equals(lbl.getText())) {
                lbl.setText(guessAnswer);
                lbl.setForeground(HIT_GREEN);
            }
        guessAnswer = null;
    }

    private void onTextChanged() {
        SwingUtilities.invokeLater(() -> {
            rehighlightAll();
            refreshPredictions(getLastWord(inputPane.getText()));
        });
    }

    private void rehighlightAll() {
        String text = inputPane.getText();
        StyledDocument doc = inputPane.getStyledDocument();

        SimpleAttributeSet def = new SimpleAttributeSet();
        StyleConstants.setForeground(def, textMain);
        doc.setCharacterAttributes(0, doc.getLength(), def, false);

        if (text.isEmpty()) return;

        String[] tokens = text.split("\\s+", -1);
        boolean endsWithSpace = text.endsWith(" ");
        int pos = 0;

        for (int i = 0; i < tokens.length; i++) {
            String tok = tokens[i];
            if (tok.isEmpty()) { pos++; continue; }

            int start = text.indexOf(tok, pos);
            if (start < 0) continue;

            String clean = tok.replaceAll("[^a-zA-Z']", "").toLowerCase();
            boolean isCurrentWord = (i == tokens.length - 1) && !endsWithSpace;

            Color colour;
            if (clean.isEmpty()) {
                colour = textMain;
            } else if (isCurrentWord) {
                colour = activeTrie.containsPrefix(clean) ? HIT_GREEN : MISS_RED;
            } else {
                colour = activeTrie.contains(clean) ? HIT_GREEN : MISS_RED;
            }

            SimpleAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setForeground(attr, colour);
            doc.setCharacterAttributes(start, tok.length(), attr, false);

            pos = start + tok.length();
        }
    }

    private void refreshPredictions(String lastWord) {
        if (lastWord == null || lastWord.isEmpty()) {
            nextCharLabel.setText("Next letter:  -");
            nextWordLabel.setText("Next word:    -");
            top5Panel.removeAll();
            top5Panel.revalidate();
            top5Panel.repaint();
            return;
        }

        String clean = lastWord.replaceAll("[^a-zA-Z']", "").toLowerCase();

        char   nc   = activeTrie.mostLikelyNextChar(clean);
        String nw   = activeTrie.mostLikelyNextWord(clean);
        Map<String, Double> top5 = activeTrie.mostLikelyNextWords(clean);

        nextCharLabel.setText("Next letter:  " + (nc == ' ' ? "-" : nc));
        nextWordLabel.setText("Next word:    " + (nw.isEmpty() ? "-" : nw));

        top5Panel.removeAll();
        if (top5.isEmpty()) {
            top5Panel.add(predLabel("  (no matches)"));
        } else {
            for (Map.Entry<String, Double> e : top5.entrySet()) {
                double pct = Math.round(e.getValue() * 10.0) / 10.0;
                JLabel lbl = predLabel(String.format("  %-20s  %.1f%%", e.getKey(), pct));
                lbl.setForeground(accent);
                top5Panel.add(lbl);
            }
        }
        top5Panel.revalidate();
        top5Panel.repaint();
    }

    private String getLastWord(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] parts = text.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private JToggleButton artistToggle(String text, Color bg, Color fg) {
        JToggleButton btn = new JToggleButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton styledButton(String text, Color fg, Color bg) {
        JButton btn = new JButton(text);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel predLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Consolas", Font.PLAIN, 14));
        lbl.setForeground(textMain);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private javax.swing.border.Border titledBorder(String title) {
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(borderC, 1), title);
        tb.setTitleColor(textSub);
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        return tb;
    }
}