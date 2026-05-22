import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.DefaultListModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Arrays;

// =============================================================
//  DATA QUALITY CHECKER — Generic Multi-Stage GUI
//  First Bank of Nigeria — Enterprise Data Management
//
//  STAGE 1: Upload CSV
//  STAGE 2: Build Validation Schema (user picks columns + validators)
//  STAGE 3: Results & Export Report
// =============================================================

public class DataQualityCheckerGUI extends JFrame {

    // ── Colours ──────────────────────────────────────────────
    private static final Color NAVY  = new Color(30,  56, 100);
    private static final Color BLUE  = new Color(46, 117, 182);
    private static final Color GOLD  = new Color(201, 168,  76);
    private static final Color GREEN = new Color(39,  140,  76);
    private static final Color RED   = new Color(192,  57,  43);
    private static final Color BG    = new Color(245, 247, 250);
    private static final Color WHITE = Color.WHITE;

    // ── Stage panels ─────────────────────────────────────────
    private JPanel     cardPanel;
    private CardLayout cardLayout;

    private StageOnePanel      stageOne;
    private StageTwoPanel      stageTwo;
    private StageThreePanel    stageThree;
    private CustomRuleManager  customRuleManager = new CustomRuleManager();

    // ── Shared state ─────────────────────────────────────────
    private String[]   headers;
    private String[][] csvData;
    private int        totalRows;
    private int        structuralFails;

    // ── Stage indicator chips (kept as fields so we can update them) ──
    private JLabel chipOne, chipTwo, chipThree;

    public DataQualityCheckerGUI() {
        setTitle("Data Quality Checker — First Bank EDM");
        setSize(1150, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(BG);

        stageOne   = new StageOnePanel();
        stageTwo   = new StageTwoPanel();
        stageThree = new StageThreePanel();

        cardPanel.add(stageOne,   "STAGE1");
        cardPanel.add(stageTwo,   "STAGE2");
        cardPanel.add(stageThree, "STAGE3");

        add(cardPanel, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        cardLayout.show(cardPanel, "STAGE1");
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────
    // HEADER
    // ─────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(14, 22, 14, 22));

        JLabel title = new JLabel("Data Quality Checker");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(WHITE);

        JLabel sub = new JLabel("Enterprise Data Management  •  First Bank of Nigeria");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 200, 230));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(title);
        text.add(sub);

        // Stage indicator chips — stored as fields for live updates
        chipOne   = stageChip("1  Upload",  true);
        chipTwo   = stageChip("2  Schema",  false);
        chipThree = stageChip("3  Results", false);

        JLabel arrow1 = arrowLabel();
        JLabel arrow2 = arrowLabel();

        JPanel stages = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        stages.setOpaque(false);
        stages.add(chipOne);
        stages.add(arrow1);
        stages.add(chipTwo);
        stages.add(arrow2);
        stages.add(chipThree);

        header.add(text,   BorderLayout.WEST);
        header.add(stages, BorderLayout.EAST);
        return header;
    }

    private JLabel arrowLabel() {
        JLabel lbl = new JLabel("›");
        lbl.setForeground(new Color(150, 170, 200));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        return lbl;
    }

    private JLabel stageChip(String text, boolean active) {
        JLabel lbl = new JLabel("  " + text + "  ");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setOpaque(true);
        lbl.setBackground(active ? GOLD : new Color(50, 70, 110));
        lbl.setForeground(active ? NAVY : new Color(160, 180, 210));
        lbl.setBorder(new EmptyBorder(4, 8, 4, 8));
        return lbl;
    }

    // ── Update which chip is highlighted ─────────────────────
    private void setActiveStage(int stage) {
        updateChip(chipOne,   stage == 1);
        updateChip(chipTwo,   stage == 2);
        updateChip(chipThree, stage == 3);
    }

    private void updateChip(JLabel chip, boolean active) {
        chip.setBackground(active ? GOLD : new Color(50, 70, 110));
        chip.setForeground(active ? NAVY : new Color(160, 180, 210));
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(230, 233, 240));
        footer.setBorder(new EmptyBorder(4, 0, 4, 0));
        JLabel lbl = new JLabel("First Bank of Nigeria  •  Enterprise Data Management  •  Data Quality Checker v1.1");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(120, 130, 150));
        footer.add(lbl);
        return footer;
    }

    // ─────────────────────────────────────────────────────────
    // NAVIGATION HELPERS
    // ─────────────────────────────────────────────────────────
    private void goToStage(String stage) {
        cardLayout.show(cardPanel, stage);
        switch (stage) {
            case "STAGE1": setActiveStage(1); break;
            case "STAGE2": setActiveStage(2); break;
            case "STAGE3": setActiveStage(3); break;
        }
    }

    // =========================================================
    //  STAGE 1 — FILE UPLOAD
    // =========================================================
    class StageOnePanel extends JPanel {

        private JTextField   pathField;
        private JLabel       statusLabel;
        private JProgressBar loadingBar;
        private JLabel       loadingMsg;
        private JPanel       loadingPanel;

        StageOnePanel() {
            setLayout(new GridBagLayout());
            setBackground(BG);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 215, 225), 1, true),
                new EmptyBorder(40, 50, 40, 50)
            ));
            card.setMaximumSize(new Dimension(600, 400));

            JLabel icon = new JLabel("📂", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
            icon.setAlignmentX(CENTER_ALIGNMENT);

            JLabel heading = new JLabel("Upload your CSV file");
            heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
            heading.setForeground(NAVY);
            heading.setAlignmentX(CENTER_ALIGNMENT);

            JLabel hint = new JLabel("The file will be scanned for structure, then you'll choose what to validate.");
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            hint.setForeground(new Color(120, 130, 150));
            hint.setAlignmentX(CENTER_ALIGNMENT);

            pathField = new JTextField("No file selected...");
            pathField.setEditable(false);
            pathField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            pathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            pathField.setBackground(new Color(248, 249, 251));
            pathField.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 210, 225)),
                new EmptyBorder(4, 10, 4, 10)
            ));

            JButton browseBtn = styledBtn("Browse", BLUE, 120, 36);
            browseBtn.addActionListener(e -> browse());
            browseBtn.setAlignmentX(CENTER_ALIGNMENT);

            statusLabel = new JLabel(" ");
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            statusLabel.setAlignmentX(CENTER_ALIGNMENT);

            JButton nextBtn = styledBtn("Load File  ›", NAVY, 180, 42);
            nextBtn.setAlignmentX(CENTER_ALIGNMENT);
            nextBtn.addActionListener(e -> loadFile());

            // Loading progress panel
            loadingBar = new JProgressBar(0, 100);
            loadingBar.setStringPainted(false);
            loadingBar.setForeground(BLUE);
            loadingBar.setBackground(new Color(220, 230, 245));
            loadingBar.setBorderPainted(false);
            loadingBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
            loadingBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));

            loadingMsg = new JLabel("Initializing...", SwingConstants.CENTER);
            loadingMsg.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            loadingMsg.setForeground(BLUE);
            loadingMsg.setAlignmentX(CENTER_ALIGNMENT);

            loadingPanel = new JPanel();
            loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.Y_AXIS));
            loadingPanel.setOpaque(false);
            loadingPanel.setAlignmentX(CENTER_ALIGNMENT);
            loadingPanel.add(loadingBar);
            loadingPanel.add(Box.createVerticalStrut(6));
            loadingPanel.add(loadingMsg);
            loadingPanel.setVisible(false);

            card.add(icon);
            card.add(Box.createVerticalStrut(12));
            card.add(heading);
            card.add(Box.createVerticalStrut(8));
            card.add(hint);
            card.add(Box.createVerticalStrut(28));
            card.add(pathField);
            card.add(Box.createVerticalStrut(10));
            card.add(browseBtn);
            card.add(Box.createVerticalStrut(14));
            card.add(statusLabel);
            card.add(Box.createVerticalStrut(10));
            card.add(loadingPanel);
            card.add(Box.createVerticalStrut(14));
            card.add(nextBtn);

            add(card);
        }

        private void browse() {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                pathField.setText(fc.getSelectedFile().getAbsolutePath());
        }

        private void loadFile() {
			String path = pathField.getText().trim();
			if (path.isEmpty() || path.equals("No file selected...")) {
				statusLabel.setForeground(RED);
				statusLabel.setText("⚠  Please select a CSV file first.");
				return;
			}

			// Show loading panel
			statusLabel.setText(" ");
			loadingBar.setValue(0);
			loadingMsg.setForeground(BLUE);
			loadingMsg.setText("Opening file...");
			loadingPanel.setVisible(true);

			// Animated messages with progress values
			String[][] messages = {
				{"Opening file...",        "5"},
				{"Reading structure...",   "20"},
				{"Scanning rows...",       "40"},
				{"Validating columns...",  "60"},
				{"Checking integrity...",  "75"},
				{"Organising data...",     "88"},
				{"Almost ready...",        "95"},
			};

			int[] step = {0};
			javax.swing.Timer animTimer = new javax.swing.Timer(400, null);
			animTimer.addActionListener(ev -> {
				if (step[0] < messages.length) {
					loadingMsg.setText(messages[step[0]][0]);
					loadingBar.setValue(Integer.parseInt(messages[step[0]][1]));
					step[0]++;
				}
			});
			animTimer.start();

			// Load file in background thread
			SwingWorker<Void, Void> loader = new SwingWorker<Void, Void>() {
				private Exception error = null;

				@Override
				protected Void doInBackground() {
					try {
						CSVFile csvFile = new CSVFile(path);
						csvFile.load();

						if (csvFile.getTotalRows() == 0) {
							error = new Exception("File is empty.");
							return null;
						}

						BufferedReader br = new BufferedReader(new FileReader(path));
						String headerLine = br.readLine();
						br.close();

						headerLine = headerLine.replace("\uFEFF", "").trim();
						if (headerLine.endsWith(","))
							headerLine = headerLine.substring(0, headerLine.length() - 1);
						headers = headerLine.split(",", -1);

						csvData         = csvFile.getCleanData();
						totalRows       = csvFile.getTotalRows();
						structuralFails = csvFile.getFailedRowCount();

					} catch (Exception ex) {
						error = ex;
					}
					return null;
				}

				@Override
				protected void done() {
					animTimer.stop();

					if (error != null) {
						loadingPanel.setVisible(false);
						statusLabel.setForeground(RED);
						statusLabel.setText("⚠  " + error.getMessage());
						return;
					}

					// Complete the bar
					loadingBar.setValue(100);
					loadingMsg.setForeground(GREEN);
					loadingMsg.setText("✓  Done!  " + totalRows + " rows · " +
						headers.length + " columns" +
						(structuralFails > 0 ? " · " + structuralFails + " structural errors removed." : " · No structural errors."));

					stageTwo.loadHeaders(headers);

					// Navigate to Stage 2 after short pause
					javax.swing.Timer doneTimer = new javax.swing.Timer(1500, ev -> {
						loadingPanel.setVisible(false);
						loadingMsg.setForeground(BLUE);
						statusLabel.setText(" ");
						goToStage("STAGE2");
					});
					doneTimer.setRepeats(false);
					doneTimer.start();
				}
			};
			loader.execute();
		}
    }

    // =========================================================
    //  STAGE 2 — SCHEMA BUILDER
    // =========================================================
    class StageTwoPanel extends JPanel {

        private JPanel schemaRows;
        private JLabel columnCountLabel;
		private JCheckBox selectAllNotEmpty;

        StageTwoPanel() {
            setLayout(new BorderLayout(0, 0));
            setBackground(BG);

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setBackground(new Color(235, 240, 250));
            topBar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(210, 215, 225)),
                new EmptyBorder(14, 22, 14, 22)
            ));

            JLabel instrTitle = new JLabel("Build Your Validation Schema");
            instrTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            instrTitle.setForeground(NAVY);

            columnCountLabel = new JLabel("");
            columnCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            columnCountLabel.setForeground(new Color(100, 110, 130));

            JLabel instr = new JLabel("Tick the validators you want to apply to each column. Leave a row blank to skip that column.");
            instr.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            instr.setForeground(new Color(80, 95, 120));

            JPanel instrLeft = new JPanel(new GridLayout(3, 1, 0, 2));
            instrLeft.setOpaque(false);
            instrLeft.add(instrTitle);
            instrLeft.add(columnCountLabel);
            instrLeft.add(instr);

            topBar.add(instrLeft, BorderLayout.WEST);

			JPanel tableHeader = buildSchemaHeader();

			schemaRows = new JPanel();
			schemaRows.setLayout(new BoxLayout(schemaRows, BoxLayout.Y_AXIS));
			schemaRows.setBackground(WHITE);
			schemaRows.setBorder(new EmptyBorder(0, 0, 20, 0));

			JScrollPane scroll = new JScrollPane(schemaRows);
			scroll.setBorder(new LineBorder(new Color(210, 215, 225)));
			scroll.getVerticalScrollBar().setUnitIncrement(16);

			JPanel center = new JPanel(new BorderLayout());
			center.setBackground(BG);
			center.setBorder(new EmptyBorder(0, 22, 0, 22));
			center.add(tableHeader, BorderLayout.NORTH);
			center.add(scroll,      BorderLayout.CENTER);

            JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
            nav.setBackground(BG);
            nav.setBorder(new MatteBorder(1, 0, 0, 0, new Color(210, 215, 225)));

            JButton backBtn = styledBtn("‹  Back", new Color(140, 150, 165), 130, 38);
            backBtn.addActionListener(e -> goToStage("STAGE1"));

            JButton customRulesBtn = styledBtn("⚙  Custom Rules", new Color(90, 110, 160), 180, 38);
            customRulesBtn.addActionListener(e -> {
                CustomRuleDialog dialog = new CustomRuleDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this), customRuleManager);
                dialog.setVisible(true);
            });

            JButton runBtn = styledBtn("Run Validation  ›", GREEN, 190, 38);
            runBtn.addActionListener(e -> runValidation());

            nav.add(backBtn);
            nav.add(customRulesBtn);
            nav.add(runBtn);

            add(topBar,  BorderLayout.NORTH);
            add(center,  BorderLayout.CENTER);
            add(nav,     BorderLayout.SOUTH);
        }

        private JPanel buildSchemaHeader() {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(NAVY);
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

            // Row 1 — Select All above NotEmpty only
            JPanel row1 = new JPanel(new GridLayout(1, 7, 0, 0));
            row1.setBackground(new Color(20, 46, 90));
            row1.setBorder(new EmptyBorder(4, 14, 4, 14));
            row1.add(new JLabel(""));

            JPanel selectAllCell = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            selectAllCell.setOpaque(false);
            selectAllNotEmpty = new JCheckBox("Select All");
            selectAllNotEmpty.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            selectAllNotEmpty.setForeground(WHITE);
            selectAllNotEmpty.setOpaque(false);
            selectAllNotEmpty.addActionListener(e -> {
                boolean sel = selectAllNotEmpty.isSelected();
                for (Component comp : schemaRows.getComponents()) {
                    if (comp instanceof SchemaRow)
                        ((SchemaRow) comp).setNotEmpty(sel);
                }
            });
            selectAllCell.add(selectAllNotEmpty);
            row1.add(selectAllCell);
            for (int i = 0; i < 5; i++) row1.add(new JLabel(""));

            // Row 2 — Column labels
            JPanel row2 = new JPanel(new GridLayout(1, 7, 0, 0));
            row2.setBackground(NAVY);
            row2.setBorder(new EmptyBorder(8, 14, 8, 14));
            String[] cols = {"Column Name", "Not Empty", "Numeric", "Email", "Date", "Length", "Custom Rule"};
            for (String c : cols) {
                JLabel lbl = new JLabel(c, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(WHITE);
                row2.add(lbl);
            }
            wrapper.add(row1, BorderLayout.NORTH);
            wrapper.add(row2, BorderLayout.SOUTH);
            return wrapper;
        }

        void loadHeaders(String[] hdrs) {
            schemaRows.removeAll();
            columnCountLabel.setText(hdrs.length + " columns detected");

            for (int i = 0; i < hdrs.length; i++) {
                schemaRows.add(new SchemaRow(hdrs[i], i));
                if (i < hdrs.length - 1)
                    schemaRows.add(new JSeparator() {{
                        setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    }});
            }

            schemaRows.revalidate();
            schemaRows.repaint();
        }

        private void runValidation() {
            Map<Integer, List<ValidatorEntry>> schema = new LinkedHashMap<>();

            for (Component comp : schemaRows.getComponents()) {
                if (comp instanceof SchemaRow) {
                    SchemaRow row = (SchemaRow) comp;
                    List<ValidatorEntry> validators = row.getSelectedValidators();
                    if (!validators.isEmpty())
                        schema.put(row.colIndex, validators);
                }
            }

            if (schema.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please select at least one validator for at least one column.",
                    "No Validators Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            stageThree.runValidation(headers, csvData, schema, totalRows, structuralFails);
            goToStage("STAGE3");
        }
    }

    // ── A single schema row ───────────────────────────────────
    class SchemaRow extends JPanel {

        final int    colIndex;
        final String colName;

        private JCheckBox  chkNotEmpty, chkNumeric, chkEmail, chkLength, chkDate, chkCustom;
		private JTextField lengthField;
		private JComboBox<String> dateFormatBox;
		private List<JCheckBox> customRuleCheckBoxes = new ArrayList<>();
		private JPopupMenu customRulePopup;
		private JLabel selectedRulesLabel;

        SchemaRow(String colName, int colIndex) {
            this.colName  = colName;
            this.colIndex = colIndex;

            setLayout(new GridLayout(1, 7, 0, 0));
            setBackground(colIndex % 2 == 0 ? WHITE : new Color(248, 250, 253));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setMinimumSize(new Dimension(0, 44));
            setBorder(new EmptyBorder(0, 14, 0, 14));

            JLabel nameLbl = new JLabel(colName);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLbl.setForeground(NAVY);

            chkNotEmpty = centeredCheckbox();
            chkNumeric  = centeredCheckbox();
            chkEmail    = centeredCheckbox();
            chkDate     = centeredCheckbox();
			chkLength   = centeredCheckbox();
			
            lengthField = new JTextField("e.g. 10");
            lengthField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lengthField.setEnabled(false);
            lengthField.setForeground(new Color(150, 150, 150));
            lengthField.setHorizontalAlignment(SwingConstants.CENTER);
            lengthField.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 210, 225)),
                new EmptyBorder(2, 6, 2, 6)
            ));
            lengthField.setMaximumSize(new Dimension(90, 28));

            chkLength.addActionListener(e -> {
				lengthField.setEnabled(chkLength.isSelected());
				if (chkLength.isSelected()) {
					lengthField.setForeground(Color.BLACK);
					lengthField.setText("");
					lengthField.requestFocus();
				} else {
					lengthField.setForeground(new Color(150, 150, 150));
					lengthField.setText("e.g. 10");
				}
			});

			// Date format dropdown
			dateFormatBox = new JComboBox<>(new String[]{
				"yyyy-MM-dd",
				"yyyy/MM/dd",
				"dd-MM-yyyy",
				"dd/MM/yyyy",
				"MM-dd-yyyy",
				"MM/dd/yyyy"
			});
			dateFormatBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
			dateFormatBox.setEnabled(false);
			dateFormatBox.setVisible(false);

			chkDate.addActionListener(e -> {
				dateFormatBox.setEnabled(chkDate.isSelected());
				dateFormatBox.setVisible(chkDate.isSelected());
				revalidate();
				repaint();
			});

			JPanel lengthPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 8));
			lengthPanel.setOpaque(false);
			lengthPanel.add(lengthField);

			// Date panel holds checkbox + dropdown together
			JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
			datePanel.setOpaque(false);
			datePanel.add(chkDate);
			datePanel.add(dateFormatBox);

			// Custom rule popup
			customRulePopup = new JPopupMenu();
			customRulePopup.setBackground(WHITE);
			customRulePopup.setBorder(new CompoundBorder(
				new LineBorder(new Color(46, 117, 182), 1),
				new EmptyBorder(4, 0, 4, 0)
			));
			// Uncheck if popup dismissed without selecting any rule
			customRulePopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
				public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
				public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
				public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
					boolean anySelected = customRuleCheckBoxes.stream()
						.anyMatch(JCheckBox::isSelected);
					if (!anySelected) {
						chkCustom.setSelected(false);
						selectedRulesLabel.setVisible(false);
					}
				}
			});

			// Label showing selected rules count/names
			selectedRulesLabel = new JLabel("None selected");
			selectedRulesLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
			selectedRulesLabel.setForeground(new Color(130, 140, 160));
			selectedRulesLabel.setVisible(false);

			chkCustom = centeredCheckbox();
			chkCustom.addActionListener(e -> {
				if (chkCustom.isSelected()) {
					refreshCustomRules();
					if (customRulePopup.getComponentCount() > 0) {
						customRulePopup.show(chkCustom, 0, chkCustom.getHeight());
					} else {
						JOptionPane.showMessageDialog(null,
							"No custom rules saved yet. Click '⚙ Custom Rules' to create some.",
							"No Rules", JOptionPane.INFORMATION_MESSAGE);
						chkCustom.setSelected(false);
					}
				} else {
					selectedRulesLabel.setVisible(false);
				}
			});

			JPanel customPanel = new JPanel(new BorderLayout(2, 2));
			customPanel.setOpaque(false);
			JPanel customTop = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 4));
			customTop.setOpaque(false);
			customTop.add(chkCustom);
			customPanel.add(customTop,           BorderLayout.NORTH);
			customPanel.add(selectedRulesLabel,  BorderLayout.CENTER);

			// Length panel holds checkbox + field together
			JPanel lengthRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
			lengthRowPanel.setOpaque(false);
			lengthRowPanel.add(chkLength);
			lengthRowPanel.add(lengthField);

			add(nameLbl);
			add(chkNotEmpty);
			add(chkNumeric);
			add(chkEmail);
			add(datePanel);
			add(lengthRowPanel);
			add(customPanel);
        }

        private JCheckBox centeredCheckbox() {
            JCheckBox cb = new JCheckBox();
            cb.setHorizontalAlignment(SwingConstants.CENTER);
            cb.setOpaque(false);
            return cb;
        }

        List<ValidatorEntry> getSelectedValidators() {
            List<ValidatorEntry> list = new ArrayList<>();
            if (chkNotEmpty.isSelected()) list.add(new ValidatorEntry("NotEmpty", 0));
            if (chkNumeric.isSelected())  list.add(new ValidatorEntry("Numeric",  0));
            if (chkEmail.isSelected())    list.add(new ValidatorEntry("Email",    0));
			if (chkDate.isSelected()) {
				String fmt = (String) dateFormatBox.getSelectedItem();
				list.add(new ValidatorEntry("Date", 0, fmt));
			}
            if (chkLength.isSelected()) {
                try {
                    int len = Integer.parseInt(lengthField.getText().trim());
                    list.add(new ValidatorEntry("Length", len));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null,
                        "Column \"" + colName + "\": Please enter a valid number for Length.",
                        "Invalid Length", JOptionPane.WARNING_MESSAGE);
                    list.clear();
                }
            }
			// Add all checked custom rules from popup
			if (chkCustom.isSelected()) {
				for (JCheckBox cb : customRuleCheckBoxes) {
					if (cb.isSelected()) {
						for (CustomRule r : customRuleManager.getRules()) {
							if (r.toString().equals(cb.getText()))
								list.add(new ValidatorEntry(r));
						}
					}
				}
			}
            return list;
        }

		void refreshCustomRules() {
			customRulePopup.removeAll();
			customRuleCheckBoxes.clear();

			// Header
			JLabel header = new JLabel("  Select Custom Rules:");
			header.setFont(new Font("Segoe UI", Font.BOLD, 11));
			header.setForeground(NAVY);
			header.setBorder(new EmptyBorder(2, 8, 6, 8));
			customRulePopup.add(header);
			customRulePopup.addSeparator();

			for (CustomRule r : customRuleManager.getRules()) {
				JCheckBox cb = new JCheckBox(r.toString());
				cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
				cb.setBackground(WHITE);
				cb.setOpaque(true);
				cb.setBorder(new EmptyBorder(5, 12, 5, 12));
				cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				cb.addMouseListener(new java.awt.event.MouseAdapter() {
					public void mouseEntered(java.awt.event.MouseEvent e) {
						cb.setBackground(new Color(235, 242, 255));
					}
					public void mouseExited(java.awt.event.MouseEvent e) {
						cb.setBackground(cb.isSelected() ? new Color(220, 235, 255) : WHITE);
					}
				});
				cb.addActionListener(e -> {
					cb.setBackground(cb.isSelected() ? new Color(220, 235, 255) : WHITE);
					updateSelectedRulesLabel();
					SwingUtilities.invokeLater(() -> customRulePopup.setVisible(true));
				});
				customRulePopup.add(cb);
				customRuleCheckBoxes.add(cb);
			}

			// Done button
			customRulePopup.addSeparator();
			JMenuItem doneItem = new JMenuItem("✔  Done");
			doneItem.setFont(new Font("Segoe UI", Font.BOLD, 11));
			doneItem.setForeground(new Color(39, 140, 76));
			doneItem.setBorder(new EmptyBorder(5, 12, 5, 12));
			doneItem.addActionListener(e -> {
				customRulePopup.setVisible(false);
				boolean anySelected = customRuleCheckBoxes.stream()
					.anyMatch(JCheckBox::isSelected);
				if (!anySelected) {
					chkCustom.setSelected(false);
					selectedRulesLabel.setVisible(false);
				} else {
					updateSelectedRulesLabel();
				}
			});
			customRulePopup.add(doneItem);
		}

		void updateSelectedRulesLabel() {
			List<String> selected = new ArrayList<>();
			for (JCheckBox cb : customRuleCheckBoxes) {
				if (cb.isSelected()) {
					// Show just the rule name (before the [ bracket)
					String txt = cb.getText();
					int bracket = txt.indexOf("  [");
					selected.add(bracket > 0 ? txt.substring(0, bracket) : txt);
				}
			}
			if (selected.isEmpty()) {
				selectedRulesLabel.setText("None selected");
				selectedRulesLabel.setForeground(new Color(130, 140, 160));
			} else {
				selectedRulesLabel.setText("<html><font color='#1A6EC7'>" +
					String.join(", ", selected) + "</font></html>");
				selectedRulesLabel.setForeground(new Color(46, 117, 182));
			}
			selectedRulesLabel.setVisible(chkCustom.isSelected());
			revalidate();
			repaint();
		}

		public void setNotEmpty(boolean selected) {
			chkNotEmpty.setSelected(selected);
		}
    }

    // ── Holds a validator type + optional parameter ───────────
    static class ValidatorEntry {
		String type;
		int    param;
		String dateFormat;
		CustomRule customRule;

		ValidatorEntry(String type, int param) {
			this.type  = type;
			this.param = param;
			this.dateFormat = "yyyy-MM-dd";
		}

		ValidatorEntry(String type, int param, String dateFormat) {
			this.type       = type;
			this.param      = param;
			this.dateFormat = dateFormat;
		}

		ValidatorEntry(CustomRule customRule) {
			this.type       = "Custom";
			this.param      = 0;
			this.dateFormat = "yyyy-MM-dd";
			this.customRule = customRule;
		}
	}

    // =========================================================
    //  STAGE 3 — RESULTS & EXPORT
    // =========================================================
    class StageThreePanel extends JPanel {

        private JTable            resultsTable;
        private DefaultTableModel tableModel;
        private JProgressBar      progressBar;
        private JLabel            totalLbl, cleanLbl, badLbl, dupLbl, rateLbl, progressLbl;
        private JButton           exportReportBtn, exportCleanBtn, exportDirtyBtn, backBtn;

        private final List<String[]> cleanRows = new ArrayList<>();
		private final List<String[]> badRows   = new ArrayList<>();
		private final Set<String>    seenRows  = new HashSet<>();
		private int duplicateCount = 0;
        private String[]       hdrs;

        StageThreePanel() {
            setLayout(new BorderLayout(0, 0));
            setBackground(BG);

            // ── Top: Progress bar ──
            JPanel topBar = new JPanel(new BorderLayout(10, 0));
            topBar.setBackground(new Color(235, 240, 250));
            topBar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(210, 215, 225)),
                new EmptyBorder(12, 22, 12, 22)
            ));

            progressLbl = new JLabel("Ready");
            progressLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            progressLbl.setForeground(NAVY);

            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(false);
            progressBar.setForeground(GOLD);
            progressBar.setBackground(new Color(210, 215, 225));
            progressBar.setBorderPainted(false);
            progressBar.setPreferredSize(new Dimension(0, 12));

            topBar.add(progressLbl, BorderLayout.NORTH);
            topBar.add(progressBar, BorderLayout.SOUTH);

            // ── Centre: Results table ──
            String[] cols = {"Row #", "Status", "Column", "Validator", "Reason", "Value Found"};
            tableModel = new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) { return false; }
            };

            resultsTable = new JTable(tableModel);
            resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            resultsTable.setRowHeight(26);
            resultsTable.setGridColor(new Color(225, 228, 235));
            resultsTable.setSelectionBackground(new Color(210, 225, 245));
            resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
            resultsTable.getTableHeader().setBackground(NAVY);
            resultsTable.getTableHeader().setForeground(WHITE);

            int[] widths = {55, 85, 140, 120, 220, 180};
            for (int i = 0; i < widths.length; i++)
                resultsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

            resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable t, Object v,
                        boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                    if (!sel) {
                        String status = String.valueOf(t.getValueAt(row, 1));
                        if ("✅ Clean".equals(status)) {
							setBackground(new Color(237, 250, 242)); // light green
						} else if ("🔁 Duplicate".equals(status)) {
							setBackground(new Color(255, 243, 205)); // light amber
						} else {
							setBackground(new Color(255, 241, 241)); // light red
						}
                    }
                    setBorder(new EmptyBorder(0, 8, 0, 8));
                    return this;
                }
            });

            JScrollPane scroll = new JScrollPane(resultsTable);
            scroll.setBorder(new LineBorder(new Color(210, 215, 225)));

            JLabel tableTitle = new JLabel("Validation Results");
            tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
            tableTitle.setForeground(NAVY);
            tableTitle.setBorder(new EmptyBorder(10, 0, 6, 0));

            JPanel center = new JPanel(new BorderLayout());
			center.setBackground(BG);
			center.setBorder(new EmptyBorder(0, 22, 0, 22));

			JPanel headerAndSelect = new JPanel(new BorderLayout());
			headerAndSelect.setOpaque(false);

			center.add(headerAndSelect, BorderLayout.NORTH);
			center.add(scroll,          BorderLayout.CENTER);

            // ── Bottom: Summary cards + buttons ──
            JPanel bottom = new JPanel(new BorderLayout(0, 0));
            bottom.setBackground(BG);
            bottom.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(210, 215, 225)),
                new EmptyBorder(10, 22, 12, 22)
            ));

            JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            cards.setOpaque(false);

            totalLbl = summaryCard("0", "Total Rows",  NAVY);
			cleanLbl = summaryCard("0", "Clean Rows",  GREEN);
			badLbl   = summaryCard("0", "Bad Rows",    RED);
			dupLbl   = summaryCard("0", "Duplicates",  new Color(201, 140, 10));
			rateLbl  = summaryCard("—", "Pass Rate",   BLUE);

			cards.add(totalLbl);
			cards.add(cleanLbl);
			cards.add(badLbl);
			cards.add(dupLbl);
			cards.add(rateLbl);

            // ── Three export buttons ──────────────────────────
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btns.setOpaque(false);

            backBtn = styledBtn("‹  New Validation", new Color(140, 150, 165), 180, 38);
            backBtn.addActionListener(e -> goToStage("STAGE2"));

            exportReportBtn = styledBtn("⬇  Full Report",   NAVY,  150, 38);
            exportCleanBtn  = styledBtn("⬇  Clean CSV",     GREEN, 140, 38);
            exportDirtyBtn  = styledBtn("⬇  Dirty CSV",     RED,   140, 38);

            exportReportBtn.setEnabled(false);
            exportCleanBtn.setEnabled(false);
            exportDirtyBtn.setEnabled(false);

            exportReportBtn.setToolTipText("Export the full validation report");
            exportCleanBtn.setToolTipText("Export only the rows that passed all validations");
            exportDirtyBtn.setToolTipText("Export only the rows that failed one or more validations");

            exportReportBtn.addActionListener(e -> exportFullReport());
            exportCleanBtn.addActionListener(e  -> exportRows(cleanRows, "Clean"));
            exportDirtyBtn.addActionListener(e  -> exportRows(badRows,   "Dirty"));

            btns.add(backBtn);
            btns.add(exportReportBtn);
            btns.add(exportCleanBtn);
            btns.add(exportDirtyBtn);

            bottom.add(cards, BorderLayout.WEST);
            bottom.add(btns,  BorderLayout.EAST);

            add(topBar,  BorderLayout.NORTH);
            add(center,  BorderLayout.CENTER);
            add(bottom,  BorderLayout.SOUTH);
        }

        // ── Called from Stage 2 ───────────────────────────────
        void runValidation(String[] headers, String[][] data,
                           Map<Integer, List<ValidatorEntry>> schema,
                           int totalRowsIn, int structFails) {

            this.hdrs = headers;
            tableModel.setRowCount(0);
			cleanRows.clear();
			badRows.clear();
			seenRows.clear();
			duplicateCount = 0;

            exportReportBtn.setEnabled(false);
            exportCleanBtn.setEnabled(false);
            exportDirtyBtn.setEnabled(false);

            progressBar.setValue(0);
            progressLbl.setText("Running validation...");
            updateCards(0, 0, 0);

            SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {
                @Override
				protected Void doInBackground() {
					int total = data.length;

					for (int i = 0; i < total; i++) {
						// Make a fresh copy of the row so we don't mutate original data
						String[] originalRow = data[i];
						String[] rowCopy = Arrays.copyOf(originalRow, originalRow.length);
						List<Object[]> rowIssues = new ArrayList<>();

						// Duplicate check — entire row as key
						String rowKey = String.join("|", rowCopy);
						if (seenRows.contains(rowKey)) {
							duplicateCount++;
							badRows.add(rowCopy);
							publish(new Object[]{
								i + 1, "🔁 Duplicate", "Entire Record",
								"Duplicate Check",
								"Record already exists — first occurrence kept",
								String.join(", ", rowCopy).substring(0, Math.min(60, String.join(", ", rowCopy).length())) + "..."
							});
							setProgress((int) ((i + 1.0) / total * 100));
							continue;
						}
						seenRows.add(rowKey);

						// PASS 1 — validate only
						for (Map.Entry<Integer, List<ValidatorEntry>> entry : schema.entrySet()) {
							int    colIdx  = entry.getKey();
							String value   = colIdx < rowCopy.length ? rowCopy[colIdx] : "";
							String colName = colIdx < headers.length ? headers[colIdx] : "Col" + colIdx;

							for (ValidatorEntry ve : entry.getValue()) {
								String reason = applyValidator(ve, value);
								if (reason != null) {
									rowIssues.add(new Object[]{
										i + 1, "❌ Bad", colName, ve.type, reason, value
									});
								}
							}
						}

						if (rowIssues.isEmpty()) {
							// PASS 2 — clean row, apply date conversion on the copy
							for (Map.Entry<Integer, List<ValidatorEntry>> entry : schema.entrySet()) {
								int colIdx = entry.getKey();
								for (ValidatorEntry ve : entry.getValue()) {
									if ("Date".equals(ve.type)) {
										String val = colIdx < rowCopy.length ? rowCopy[colIdx] : "";
										String converted = convertDate(val, ve.dateFormat);
										rowCopy[colIdx] = converted;
									}
								}
							}
							cleanRows.add(rowCopy);
							publish(new Object[]{i + 1, "✅ Clean", "—", "—", "—", "—"});
						} else {
							badRows.add(rowCopy);
							publish(rowIssues.get(0));
							for (int j = 1; j < rowIssues.size(); j++) {
								Object[] issue = rowIssues.get(j);
								publish(new Object[]{
									issue[0], "  ↳", issue[2], issue[3], issue[4], issue[5]
								});
							}
						}

						setProgress((int) ((i + 1.0) / total * 100));
					}
					return null;
				}

                @Override
				protected void process(List<Object[]> chunks) {
					for (Object[] r : chunks) tableModel.addRow(r);
					progressBar.setValue(getProgress());
					int total = cleanRows.size() + badRows.size();
					updateCards(total, cleanRows.size(), badRows.size());
				}

                @Override
				protected void done() {
					progressBar.setValue(100);
					int total = cleanRows.size() + badRows.size();
					progressLbl.setText("Validation complete ✓  —  " +
					total + " rows validated. " +
					duplicateCount + " duplicate(s) found. " +
					(structFails > 0
						? structFails + " structural rows removed before validation."
						: "No structural errors."));
					exportReportBtn.setEnabled(true);
					exportCleanBtn.setEnabled(!cleanRows.isEmpty());
					exportDirtyBtn.setEnabled(!badRows.isEmpty());
					updateCards(total, cleanRows.size(), badRows.size());
				}
            };

            worker.addPropertyChangeListener(e -> {
                if ("progress".equals(e.getPropertyName()))
                    progressBar.setValue((Integer) e.getNewValue());
            });
            worker.execute();
        }

        // ── Real validator classes wired in ───────────────────
        private String applyValidator(ValidatorEntry ve, String value) {
            switch (ve.type) {

                case "NotEmpty": {
                    NotEmptyValidator v = new NotEmptyValidator();
                    if (!v.validate(value))
                        return "Value is empty or null";
                    break;
                }

                case "Numeric": {
                    // Skip blank — let NotEmpty handle that separately
                    if (value == null || value.trim().isEmpty()) return null;
                    NumericValidator v = new NumericValidator();
                    if (!v.validate(value))
                        return "\"" + value.trim() + "\" is not a valid number";
                    break;
                }

                case "Email": {
                    if (value == null || value.trim().isEmpty()) return null;
                    EmailValidator v = new EmailValidator();
                    if (!v.validate(value.trim()))
                        return "\"" + value.trim() + "\" is not a valid email address";
                    break;
                }
				
				case "Date": {
					DateValidator v = new DateValidator();
					if (!v.validate(value))
						return "\"" + (value == null ? "" : value.trim()) + "\" is not a valid date";
					break;
				}
				
                case "Length": {
                    LengthValidator v = new LengthValidator();
                    v.setValidLength(ve.param);
                    if (!v.validate(value == null ? "" : value))
                        return "Expected length " + ve.param +
                               ", found " + (value == null ? 0 : value.length());
                    break;
                }

                case "Custom": {
                    if (ve.customRule == null) break;
                    String result = applyCustomRule(ve.customRule, value);
                    if (result != null) return result;
                    break;
                }
            }
            return null; // null = passed
        }

        private String applyCustomRule(CustomRule rule, String value) {
            String val     = value == null ? "" : value.trim();
            String ruleVal = rule.value == null ? "" : rule.value.trim();
            String[] parts = ruleVal.split(",");

            switch (rule.type) {
                case "Starts With": {
                    for (String p : parts)
                        if (val.startsWith(p.trim())) return null;
                    return "\"" + val + "\" does not start with any of: " + ruleVal;
                }
                case "Ends With": {
                    for (String p : parts)
                        if (val.endsWith(p.trim())) return null;
                    return "\"" + val + "\" does not end with any of: " + ruleVal;
                }
                case "Contains": {
                    for (String p : parts)
                        if (val.contains(p.trim())) return null;
                    return "\"" + val + "\" does not contain any of: " + ruleVal;
                }
                case "Does Not Contain": {
                    for (String p : parts)
                        if (val.contains(p.trim()))
                            return "\"" + val + "\" contains forbidden value: " + p.trim();
                    return null;
                }
                case "Must Be One Of": {
                    for (String p : parts)
                        if (val.equalsIgnoreCase(p.trim())) return null;
                    return "\"" + val + "\" is not one of: " + ruleVal;
                }
                case "Must Not Be One Of": {
                    for (String p : parts)
                        if (val.equalsIgnoreCase(p.trim()))
                            return "\"" + val + "\" is not allowed. Forbidden values: " + ruleVal;
                    return null;
                }
                case "Greater Than": {
                    try {
                        double num    = Double.parseDouble(val);
                        double target = Double.parseDouble(ruleVal);
                        if (num <= target)
                            return "\"" + val + "\" is not greater than " + ruleVal;
                    } catch (NumberFormatException e) {
                        return "\"" + val + "\" is not a valid number for Greater Than check";
                    }
                    return null;
                }
                case "Less Than": {
                    try {
                        double num    = Double.parseDouble(val);
                        double target = Double.parseDouble(ruleVal);
                        if (num >= target)
                            return "\"" + val + "\" is not less than " + ruleVal;
                    } catch (NumberFormatException e) {
                        return "\"" + val + "\" is not a valid number for Less Than check";
                    }
                    return null;
                }
                case "Between": {
                    try {
                        double num = Double.parseDouble(val);
                        double min = Double.parseDouble(parts[0].trim());
                        double max = Double.parseDouble(parts.length > 1 ? parts[1].trim() : parts[0].trim());
                        if (num < min || num > max)
                            return "\"" + val + "\" is not between " + parts[0].trim() + " and " + (parts.length > 1 ? parts[1].trim() : parts[0].trim());
                    } catch (NumberFormatException e) {
                        return "\"" + val + "\" is not a valid number for Between check";
                    }
                    return null;
                }
                case "Minimum Length": {
                    try {
                        int min = Integer.parseInt(ruleVal);
                        if (val.length() < min)
                            return "\"" + val + "\" is shorter than minimum length " + min;
                    } catch (NumberFormatException e) {
                        return "Invalid minimum length value: " + ruleVal;
                    }
                    return null;
                }
                case "Maximum Length": {
                    try {
                        int max = Integer.parseInt(ruleVal);
                        if (val.length() > max)
                            return "\"" + val + "\" exceeds maximum length " + max;
                    } catch (NumberFormatException e) {
                        return "Invalid maximum length value: " + ruleVal;
                    }
                    return null;
                }
                case "Exact Length": {
                    try {
                        int exact = Integer.parseInt(ruleVal);
                        if (val.length() != exact)
                            return "Expected exact length " + exact + ", found " + val.length();
                    } catch (NumberFormatException e) {
                        return "Invalid exact length value: " + ruleVal;
                    }
                    return null;
                }
                case "Matches Pattern (Regex)": {
                    try {
                        if (!val.matches(ruleVal))
                            return "\"" + val + "\" does not match pattern: " + ruleVal;
                    } catch (Exception e) {
                        return "Invalid regex pattern: " + ruleVal;
                    }
                    return null;
                }
                case "Must Be Uppercase":
                    if (!val.equals(val.toUpperCase()))
                        return "\"" + val + "\" is not uppercase";
                    return null;
                case "Must Be Lowercase":
                    if (!val.equals(val.toLowerCase()))
                        return "\"" + val + "\" is not lowercase";
                    return null;
                case "No Special Characters":
                    if (!val.matches("[a-zA-Z0-9 ]*"))
                        return "\"" + val + "\" contains special characters";
                    return null;
                case "No Whitespace":
                    if (val.contains(" "))
                        return "\"" + val + "\" contains whitespace";
                    return null;
                default:
                    return null;
            }
        }
		
		private String convertDate(String value, String targetFormat) {
			if (value == null || value.trim().isEmpty()) return value;

			java.time.format.DateTimeFormatter[] parsers = {
				java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
				java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
				java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"),
				java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
				java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
				java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy")
			};

			java.time.format.DateTimeFormatter target =
				java.time.format.DateTimeFormatter.ofPattern(targetFormat);

			for (java.time.format.DateTimeFormatter parser : parsers) {
				try {
					java.time.LocalDate date =
						java.time.LocalDate.parse(value.trim(), parser);
					return date.format(target);
				} catch (java.time.format.DateTimeParseException ignored) {}
			}
			return value; // return original if conversion fails
		}

        // ── Export full validation report ─────────────────────
        private void exportFullReport() {
            File file = chooseSaveFile("DQC_Report.csv");
            if (file == null) return;

            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("DATA QUALITY CHECKER — FULL REPORT");
                pw.println("First Bank of Nigeria — Enterprise Data Management");
                pw.println("Generated:," + new java.util.Date());
                pw.println();

                int total = cleanRows.size() + badRows.size();
                pw.println("Total Rows Validated," + total);
                pw.println("Clean Rows,"           + cleanRows.size());
                pw.println("Bad Rows,"             + badRows.size());
                pw.printf("Pass Rate,%.1f%%%n",
                    total > 0 ? (cleanRows.size() * 100.0 / total) : 0.0);
                pw.println();

                pw.println("Row #,Status,Column,Validator,Reason,Value Found");
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        if (j > 0) sb.append(",");
                        sb.append("\"").append(tableModel.getValueAt(i, j)).append("\"");
                    }
                    pw.println(sb);
                }

                showExportSuccess(file);
            } catch (IOException ex) {
                showExportError(ex);
            }
        }

        // ── Export clean or dirty rows as plain CSV ───────────
        private void exportRows(List<String[]> rows, String label) {
            if (rows.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No " + label.toLowerCase() + " rows to export.",
                    "Nothing to Export", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            File file = chooseSaveFile("DQC_" + label + "_Rows.csv");
            if (file == null) return;

            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                // Write header row
                pw.println(String.join(",", hdrs));

                // Write data rows
                for (String[] row : rows) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < row.length; j++) {
                        if (j > 0) sb.append(",");
                        String cell = row[j] == null ? "" : row[j];
                        // Wrap in quotes if the value contains a comma
                        if (cell.contains(",") || cell.contains("\"")) {
                            cell = "\"" + cell.replace("\"", "\"\"") + "\"";
                        }
                        sb.append(cell);
                    }
                    pw.println(sb);
                }

                showExportSuccess(file);
            } catch (IOException ex) {
                showExportError(ex);
            }
        }

        // ── File chooser helper ───────────────────────────────
        private File chooseSaveFile(String defaultName) {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(defaultName));
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
            return fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
        }

        private void showExportSuccess(File file) {
            JOptionPane.showMessageDialog(this,
                "File saved to:\n" + file.getAbsolutePath(),
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        }

        private void showExportError(IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Export failed: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }

        // ── Summary cards ─────────────────────────────────────
        private void updateCards(int total, int clean, int bad) {
			setCard(totalLbl, String.valueOf(total), "Total Rows");
			setCard(cleanLbl, String.valueOf(clean), "Clean Rows");
			setCard(badLbl,   String.valueOf(bad),   "Bad Rows");
			setCard(dupLbl,   String.valueOf(duplicateCount), "Duplicates");
			String rate = total > 0
				? String.format("%.1f%%", clean * 100.0 / total) : "—";
			setCard(rateLbl, rate, "Pass Rate");
		}

        private void setCard(JLabel lbl, String val, String sub) {
            lbl.setText("<html><center><b style='font-size:18px'>" + val +
                "</b><br/><span style='font-size:10px'>" + sub + "</span></center></html>");
        }
    }

    // =========================================================
    //  CUSTOM RULE
    // =========================================================
    static class CustomRule {
        String name;
        String type;
        String value;

        CustomRule(String name, String type, String value) {
            this.name  = name;
            this.type  = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return name + "  [" + type + "]";
        }
    }

    // =========================================================
    //  CUSTOM RULE MANAGER
    // =========================================================
    static class CustomRuleManager {
        private static final String FILE = "custom_rules.txt";
        private List<CustomRule> rules = new ArrayList<>();

        CustomRuleManager() { load(); }

        void load() {
            rules.clear();
            File f = new File(FILE);
            if (!f.exists()) return;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|\\|", 3);
                    if (parts.length == 3)
                        rules.add(new CustomRule(parts[0], parts[1], parts[2]));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        void save() {
            try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
                for (CustomRule r : rules)
                    pw.println(r.name + "||" + r.type + "||" + r.value);
            } catch (Exception e) { e.printStackTrace(); }
        }

        void addRule(CustomRule r)   { rules.add(r); save(); }
        void deleteRule(int index)   { rules.remove(index); save(); }
        List<CustomRule> getRules()  { return rules; }
    }

    // =========================================================
    //  CUSTOM RULE DIALOG
    // =========================================================
    class CustomRuleDialog extends JDialog {

        private final CustomRuleManager manager;
        private DefaultListModel<CustomRule> listModel;
        private JList<CustomRule> ruleList;
        private JTextField  nameField;
        private JComboBox<String> typeBox;
        private JTextArea   valueArea;
        private JLabel      valueHint;
        private JLabel      formTitle;
        private JButton     saveBtn;
        private int         editingIndex = -1;

        CustomRuleDialog(Frame owner, CustomRuleManager manager) {
            super(owner, "Manage Custom Validation Rules", true);
            this.manager = manager;

            setSize(820, 520);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());
            getContentPane().setBackground(BG);

            // Title bar
            JPanel titleBar = new JPanel(new BorderLayout());
            titleBar.setBackground(NAVY);
            titleBar.setBorder(new EmptyBorder(14, 20, 14, 20));
            JLabel titleLbl = new JLabel("Custom Validation Rules");
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLbl.setForeground(WHITE);
            JLabel subLbl = new JLabel("Create reusable rules to apply across any column");
            subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            subLbl.setForeground(new Color(180, 195, 220));
            JPanel titleText = new JPanel(new GridLayout(2, 1, 0, 2));
            titleText.setOpaque(false);
            titleText.add(titleLbl);
            titleText.add(subLbl);
            titleBar.add(titleText, BorderLayout.WEST);
            add(titleBar, BorderLayout.NORTH);

            // Main split panel
            JPanel main = new JPanel(new GridLayout(1, 2, 16, 0));
            main.setBackground(BG);
            main.setBorder(new EmptyBorder(16, 16, 16, 16));

            // LEFT — saved rules list
            JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
            leftPanel.setOpaque(false);

            JLabel savedLbl = new JLabel("Saved Rules");
            savedLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            savedLbl.setForeground(NAVY);

            listModel = new DefaultListModel<>();
            for (CustomRule r : manager.getRules()) listModel.addElement(r);

            ruleList = new JList<>(listModel);
            ruleList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            ruleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            ruleList.setBorder(new EmptyBorder(6, 8, 6, 8));

            JScrollPane listScroll = new JScrollPane(ruleList);
            listScroll.setBorder(new LineBorder(new Color(210, 215, 225)));

            JButton editBtn = styledBtn("✏  Edit", BLUE, 95, 36);
            editBtn.addActionListener(e -> {
                int idx = ruleList.getSelectedIndex();
                if (idx >= 0) {
                    CustomRule r = listModel.get(idx);
                    editingIndex = idx;
                    nameField.setText(r.name);
                    for (int i = 0; i < typeBox.getItemCount(); i++) {
                        if (typeBox.getItemAt(i).equals(r.type)) {
                            typeBox.setSelectedIndex(i);
                            break;
                        }
                    }
                    valueArea.setText(r.value);
                    formTitle.setText("Edit Rule");
                    saveBtn.setText("✔  Update Rule");
                    updateHint();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Please select a rule to edit.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                }
            });

            JButton deleteBtn = styledBtn("🗑  Delete", RED, 95, 36);
            deleteBtn.addActionListener(e -> {
                int idx = ruleList.getSelectedIndex();
                if (idx >= 0) {
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Delete rule \"" + listModel.get(idx).name + "\"?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        manager.deleteRule(idx);
                        listModel.remove(idx);
                        if (editingIndex == idx) resetForm();
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Please select a rule to delete.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                }
            });

            JPanel leftBtns = new JPanel(new GridLayout(1, 2, 6, 0));
            leftBtns.setOpaque(false);
            leftBtns.add(editBtn);
            leftBtns.add(deleteBtn);

            leftPanel.add(savedLbl,   BorderLayout.NORTH);
            leftPanel.add(listScroll, BorderLayout.CENTER);
            leftPanel.add(leftBtns,   BorderLayout.SOUTH);

            // RIGHT — rule creation form
            JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
            rightPanel.setOpaque(false);

            formTitle = new JLabel("Create New Rule");
            formTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
            formTitle.setForeground(NAVY);

            JPanel form = new JPanel(new GridLayout(0, 1, 0, 10));
            form.setOpaque(false);

            JLabel nameLbl = new JLabel("Rule Name:");
            nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            nameField = new JTextField();
            nameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            nameField.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 210, 225)),
                new EmptyBorder(4, 8, 4, 8)
            ));

            JLabel typeLbl = new JLabel("Rule Type:");
            typeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            String[] types = {
                "Starts With", "Ends With", "Contains", "Does Not Contain",
                "Must Be One Of", "Must Not Be One Of",
                "Greater Than", "Less Than", "Between",
                "Minimum Length", "Maximum Length", "Exact Length",
                "Matches Pattern (Regex)",
                "Must Be Uppercase", "Must Be Lowercase",
                "No Special Characters", "No Whitespace"
            };
            typeBox = new JComboBox<>(types);
            typeBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            JLabel valLbl = new JLabel("Value(s):");
            valLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            valueHint = new JLabel("Enter value(s) separated by commas");
            valueHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            valueHint.setForeground(new Color(130, 140, 160));

            valueArea = new JTextArea(3, 20);
            valueArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            valueArea.setLineWrap(true);
            valueArea.setWrapStyleWord(true);
            valueArea.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 210, 225)),
                new EmptyBorder(4, 8, 4, 8)
            ));
            JScrollPane valueScroll = new JScrollPane(valueArea);
            valueScroll.setBorder(null);

            typeBox.addActionListener(e -> updateHint());
            updateHint();

            form.add(nameLbl);
            form.add(nameField);
            form.add(typeLbl);
            form.add(typeBox);
            form.add(valLbl);
            form.add(valueHint);
            form.add(valueScroll);

            saveBtn = styledBtn("＋  Save Rule", GREEN, 150, 36);
            saveBtn.addActionListener(e -> saveRule());

            JButton cancelBtn = styledBtn("✕  Cancel", new Color(140, 150, 165), 120, 36);
            cancelBtn.addActionListener(e -> resetForm());

            JPanel formBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            formBtns.setOpaque(false);
            formBtns.add(saveBtn);
            formBtns.add(cancelBtn);

            rightPanel.add(formTitle, BorderLayout.NORTH);
            rightPanel.add(form,      BorderLayout.CENTER);
            rightPanel.add(formBtns,  BorderLayout.SOUTH);

            main.add(leftPanel);
            main.add(rightPanel);
            add(main, BorderLayout.CENTER);

            // Close button
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
            bottom.setBackground(BG);
            bottom.setBorder(new MatteBorder(1, 0, 0, 0, new Color(210, 215, 225)));
            JButton closeBtn = styledBtn("Close", new Color(140, 150, 165), 120, 36);
            closeBtn.addActionListener(e -> dispose());
            bottom.add(closeBtn);
            add(bottom, BorderLayout.SOUTH);
        }

        private void updateHint() {
            String type = (String) typeBox.getSelectedItem();
            switch (type) {
                case "Starts With": case "Ends With": case "Contains":
                case "Does Not Contain": case "Must Be One Of": case "Must Not Be One Of":
                    valueHint.setText("Enter one or more values separated by commas e.g. 300,400,500");
                    break;
                case "Greater Than": case "Less Than":
                case "Minimum Length": case "Maximum Length": case "Exact Length":
                    valueHint.setText("Enter a single numeric value e.g. 11");
                    break;
                case "Between":
                    valueHint.setText("Enter two numeric values separated by comma e.g. 1,100");
                    break;
                case "Matches Pattern (Regex)":
                    valueHint.setText("Enter a valid regex pattern e.g. ^[A-Z]{2}[0-9]{8}$");
                    break;
                case "Must Be Uppercase": case "Must Be Lowercase":
                case "No Special Characters": case "No Whitespace":
                    valueHint.setText("No value needed — leave blank");
                    valueArea.setText("");
                    break;
                default:
                    valueHint.setText("Enter value(s)");
            }
        }

        private void saveRule() {
            String name  = nameField.getText().trim();
            String type  = (String) typeBox.getSelectedItem();
            String value = valueArea.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a rule name.", "Missing Name",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean needsValue = !type.equals("Must Be Uppercase") &&
                                 !type.equals("Must Be Lowercase") &&
                                 !type.equals("No Special Characters") &&
                                 !type.equals("No Whitespace");

            if (needsValue && value.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a value for this rule type.", "Missing Value",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            CustomRule rule = new CustomRule(name, type, value);

            if (editingIndex >= 0) {
                manager.getRules().set(editingIndex, rule);
                manager.save();
                listModel.set(editingIndex, rule);
                JOptionPane.showMessageDialog(this,
                    "Rule \"" + name + "\" updated successfully!",
                    "Rule Updated", JOptionPane.INFORMATION_MESSAGE);
                resetForm();
            } else {
                manager.addRule(rule);
                listModel.addElement(rule);
                JOptionPane.showMessageDialog(this,
                    "Rule \"" + name + "\" saved successfully!",
                    "Rule Saved", JOptionPane.INFORMATION_MESSAGE);
                nameField.setText("");
                valueArea.setText("");
                typeBox.setSelectedIndex(0);
            }
        }

        private void resetForm() {
            editingIndex = -1;
            nameField.setText("");
            valueArea.setText("");
            typeBox.setSelectedIndex(0);
            formTitle.setText("Create New Rule");
            saveBtn.setText("＋  Save Rule");
            ruleList.clearSelection();
            updateHint();
        }
    }

    // =========================================================
    //  SHARED UI HELPERS
    // =========================================================
    private JButton styledBtn(String text, Color bg, int w, int h) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, h));
        return btn;
    }

    private JLabel summaryCard(String val, String sub, Color bg) {
        JLabel lbl = new JLabel(
            "<html><center><b style='font-size:18px'>" + val +
            "</b><br/><span style='font-size:10px'>" + sub + "</span></center></html>",
            SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(WHITE);
        lbl.setBackground(bg);
        lbl.setOpaque(true);
        lbl.setPreferredSize(new Dimension(120, 54));
        lbl.setBorder(new EmptyBorder(4, 8, 4, 8));
        return lbl;
    }
	
    // =========================================================
    //  MAIN
    // =========================================================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(DataQualityCheckerGUI::new);
    }
}