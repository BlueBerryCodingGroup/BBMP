package net.bbmp;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PacketMonitor {
    private static final PacketMonitor INSTANCE = new PacketMonitor();
    public static PacketMonitor get() { return INSTANCE; }
    private final JFrame frame;
    private final PacketTableModel model;
    private final JComboBox<String> filterBox;
    private final JTextField searchField;
    private final JToggleButton pauseBtn;
    private final JToggleButton autoscrollBtn;
    private final JLabel counters;
    private volatile boolean paused = false;
    private volatile boolean autoscroll = true;
    private long bytesC2S = 0;
    private long bytesS2C = 0;

    public PacketMonitor() {
        FlatDarkLaf.setup();
        model = new PacketTableModel();
        frame = new JFrame("BBMP Monitor");
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setRowHeight(22);
        table.setForeground(new Color(230,230,230));
        table.setBackground(new Color(32,32,36));
        table.setGridColor(new Color(64,64,70));
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(24,24,28));
        frame.getContentPane().setBackground(new Color(24,24,28));
        frame.setLayout(new BorderLayout());
        frame.add(scroll, BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(new Color(24,24,28));
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> { model.clear(); bytesC2S = 0; bytesS2C = 0; updateCounters(); });
        top.add(clear);
        filterBox = new JComboBox<>(new String[]{"All", "Handshake/Status", "Raw only"});
        filterBox.addActionListener(e -> {
            int idx = filterBox.getSelectedIndex();
            switch (idx) {
                case 1 -> model.setFilterMode(FilterMode.HANDSHAKE_ONLY);
                case 2 -> model.setFilterMode(FilterMode.RAW_ONLY);
                default -> model.setFilterMode(FilterMode.ALL);
            }
        });
        top.add(new JLabel("Filter"));
        top.add(filterBox);
        searchField = new JTextField(20);
        searchField.addActionListener(e -> model.setSearch(searchField.getText().trim()));
        top.add(new JLabel("Search"));
        top.add(searchField);
        pauseBtn = new JToggleButton("Pause");
        pauseBtn.addActionListener(e -> paused = pauseBtn.isSelected());
        top.add(pauseBtn);
        autoscrollBtn = new JToggleButton("Autoscroll", true);
        autoscrollBtn.addActionListener(e -> autoscroll = autoscrollBtn.isSelected());
        top.add(autoscrollBtn);
        JButton export = new JButton(new AbstractAction("Export CSV") {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
                        w.write("time,dir,packet,id,length,note,kind");
                        for (PacketRow r : model.getAll()) {
                            w.write(String.join("," ,
                                    r.time, r.dir, r.name, (r.packetId<0?"-":String.format("0x%02X", r.packetId)),
                                    String.valueOf(r.length), r.note.replace(","," "), r.tag.name()));
                            w.write("\n");
                        }
                    } catch (Exception ex) { }
                }
            }
        });
        top.add(export);
        counters = new JLabel("C→S 0 B | S→C 0 B");
        counters.setForeground(new Color(180,180,190));
        top.add(counters);
        frame.add(top, BorderLayout.NORTH);

        Timer t = new Timer(500, e -> {
            if (autoscroll && table.getRowCount() > 0) table.scrollRectToVisible(table.getCellRect(table.getRowCount()-1, 0, true));
        });
        t.start();

        frame.setSize(1100, 600);
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    }

    public void showWindow() { frame.setVisible(true); }
    public static void add(String dir, String name, int packetId, int length, String note, Tag tag) {
        PacketMonitor m = get();
        if (m.paused) return;
        if (dir.startsWith("C")) m.bytesC2S += Math.max(0, length);
        else m.bytesS2C += Math.max(0, length);
        m.updateCounters();
        m.model.addRow(new PacketRow(dir, name, packetId, length, note, tag));
    }
    public static void log(String s) { get().model.addRow(new PacketRow("-", "INFO", -1, -1, s, Tag.HANDSHAKE)); }
    private void updateCounters() { counters.setText(String.format("C→S %s | S→C %s", human(bytesC2S), human(bytesS2C))); }
    private static String human(long b) {
        if (b < 1024) return b + " B";
        double kb = b / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }
    enum FilterMode { ALL, HANDSHAKE_ONLY, RAW_ONLY }
    static class PacketRow {
        final String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        final String dir, name, note;
        final int packetId, length;
        final Tag tag;
        PacketRow(String dir, String name, int packetId, int length, String note, Tag tag) {
            this.dir = dir; this.name = name; this.packetId = packetId; this.length = length; this.note = note == null ? "" : note; this.tag = tag;
        }
    }
    static class PacketTableModel extends AbstractTableModel {
        private final List<PacketRow> allRows = new ArrayList<>();
        private final List<PacketRow> view = new ArrayList<>();
        private final String[] cols = {"Time", "Dir", "Packet", "PacketID", "Length", "Note", "Kind"};
        private FilterMode mode = FilterMode.ALL;
        private String search = "";
        public int getRowCount() { return view.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) {
            PacketRow x = view.get(r);
            return switch (c) {
                case 0 -> x.time;
                case 1 -> x.dir;
                case 2 -> x.name;
                case 3 -> (x.packetId < 0 ? "-" : String.format("0x%02X", x.packetId));
                case 4 -> (x.length < 0 ? "-" : x.length);
                case 5 -> x.note;
                case 6 -> x.tag.name();
                default -> "";
            };
        }
        void addRow(PacketRow r) {
            allRows.add(r);
            if (matches(r)) {
                int i = view.size();
                view.add(r);
                fireTableRowsInserted(i, i);
            }
        }
        void clear() { allRows.clear(); view.clear(); fireTableDataChanged(); }
        List<PacketRow> getAll() { return allRows; }
        void setFilterMode(FilterMode m) { mode = m; refilter(); }
        void setSearch(String s) { search = s == null ? "" : s.toLowerCase(); refilter(); }
        private void refilter() {
            view.clear();
            for (PacketRow r : allRows) if (matches(r)) view.add(r);
            fireTableDataChanged();
        }
        private boolean matches(PacketRow r) {
            boolean modeOk = switch (mode) {
                case ALL -> true;
                case HANDSHAKE_ONLY -> r.tag == Tag.HANDSHAKE;
                case RAW_ONLY -> r.tag == Tag.RAW;
            };
            if (!modeOk) return false;
            if (search.isEmpty()) return true;
            return (r.name.toLowerCase().contains(search) || r.note.toLowerCase().contains(search) || r.dir.toLowerCase().contains(search));
        }
    }
}