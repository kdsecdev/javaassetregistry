import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FixedAssetsRegistry {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private Connection connection;
    private JTextField searchField;
    private JComboBox<String> filterCategoryCombo;
    private JComboBox<String> filterStatusCombo;
    private JLabel totalAssetsLabel;
    private JLabel totalValueLabel;

    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/asset_registry_db";
    private static final String DB_USER = "username"; // Change as needed
    private static final String DB_PASSWORD = "password"; // Change as needed

    // Asset class to represent fixed assets
    static class Asset {
        private int id;
        private final String name;
        private final String category;
        private final String description;
        private final double cost;
        private final LocalDate purchaseDate;
        private final String location;
        private final String status;
        private final String serialNumber;
        private final String supplier;
        private final LocalDate warrantyExpiry;
        private final double depreciationRate;

        public Asset(int id, String name, String category, String description,
                     double cost, LocalDate purchaseDate, String location, String status,
                     String serialNumber, String supplier, LocalDate warrantyExpiry, double depreciationRate) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.description = description;
            this.cost = cost;
            this.purchaseDate = purchaseDate;
            this.location = location;
            this.status = status;
            this.serialNumber = serialNumber;
            this.supplier = supplier;
            this.warrantyExpiry = warrantyExpiry;
            this.depreciationRate = depreciationRate;
        }

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public double getCost() { return cost; }
        public LocalDate getPurchaseDate() { return purchaseDate; }
        public String getLocation() { return location; }
        public String getStatus() { return status; }
        public String getSerialNumber() { return serialNumber; }
        public String getSupplier() { return supplier; }
        public LocalDate getWarrantyExpiry() { return warrantyExpiry; }
        public double getDepreciationRate() { return depreciationRate; }

        public Object[] toTableRow() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return new Object[]{
                    id, name, category, description,
                    String.format("$%.2f", cost),
                    purchaseDate.format(formatter),
                    location, status, serialNumber, supplier,
                    warrantyExpiry != null ? warrantyExpiry.format(formatter) : "N/A"
            };
        }
    }

    public FixedAssetsRegistry() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            initializeDatabase();
            initializeGUI();
            loadAssets();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Database connection failed: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("Connected to MySQL database successfully!");
    }

    private void initializeGUI() {
        frame = new JFrame("Fixed Assets Registry - Database Edition");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 700);
        frame.setLocationRelativeTo(null);

        // Create menu bar
        createMenuBar();

        // Create search and filter panel
        JPanel searchPanel = createSearchPanel();

        // Create table
        String[] columnNames = {"ID", "Name", "Category", "Description", "Cost",
                "Purchase Date", "Location", "Status", "Serial No.", "Supplier", "Warranty"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);

        // Set column widths
        int[] columnWidths = {50, 120, 100, 180, 80, 100, 120, 80, 100, 120, 100};
        for (int i = 0; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }

        JScrollPane scrollPane = new JScrollPane(table);

        // Create button panel
       JPanel buttonPanel = createButtonPanel();

        // Create status panel
        JPanel statusPanel = createStatusPanel();

        // Layout
        frame.setLayout(new BorderLayout());
        frame.add(searchPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.add(statusPanel, BorderLayout.EAST);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export to CSV");
        JMenuItem exitItem = new JMenuItem("Exit");

        exportItem.addActionListener(_ -> exportToCSV());
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Reports menu
        JMenu reportsMenu = new JMenu("Reports");
        JMenuItem summaryItem = new JMenuItem("Asset Summary");
        JMenuItem depreciationItem = new JMenuItem("Depreciation Report");

        summaryItem.addActionListener(e -> showAssetSummary());
        depreciationItem.addActionListener(e -> showDepreciationReport());

        reportsMenu.add(summaryItem);
        reportsMenu.add(depreciationItem);

        menuBar.add(fileMenu);
        menuBar.add(reportsMenu);

        frame.setJMenuBar(menuBar);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Search & Filter"));

        // Search field
        panel.add(new JLabel("Search:"));
        searchField = new JTextField(15);
        searchField.addActionListener(e -> filterTable());
        panel.add(searchField);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> filterTable());
        panel.add(searchButton);

        // Category filter
        panel.add(new JLabel("Category:"));
        filterCategoryCombo = new JComboBox<>();
        filterCategoryCombo.addItem("All Categories");
        loadCategories(filterCategoryCombo);
        filterCategoryCombo.addActionListener(e -> filterTable());
        panel.add(filterCategoryCombo);

        // Status filter
        panel.add(new JLabel("Status:"));
        filterStatusCombo = new JComboBox<>();
        filterStatusCombo.addItem("All Statuses");
        loadStatuses(filterStatusCombo);
        filterStatusCombo.addActionListener(e -> filterTable());
        panel.add(filterStatusCombo);

        JButton clearButton = new JButton("Clear Filters");
        clearButton.addActionListener(e -> clearFilters());
        panel.add(clearButton);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JButton addButton = new JButton("Add Asset");
        JButton editButton = new JButton("Edit Asset");
        JButton deleteButton = new JButton("Delete Asset");
        JButton refreshButton = new JButton("Refresh");
        JButton duplicateButton = new JButton("Duplicate Asset");

        addButton.addActionListener(e -> showAddAssetDialog());
        editButton.addActionListener(e -> showEditAssetDialog());
        deleteButton.addActionListener(e -> deleteSelectedAsset());
        refreshButton.addActionListener(e -> loadAssets());
        duplicateButton.addActionListener(e -> duplicateSelectedAsset());

        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(duplicateButton);
        panel.add(refreshButton);

        return panel;
    }

   private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createLoweredBevelBorder());

        totalAssetsLabel = new JLabel("Total Assets: 0");
        totalValueLabel = new JLabel("Total Value: $0.00");

        panel.add(totalAssetsLabel);
        panel.add(Box.createRigidArea(new Dimension(20, 0)));
        panel.add(totalValueLabel);

        return panel;
    }

    private void loadCategories(JComboBox<String> combo) {
        try {
            String sql = "SELECT category_name FROM asset_categories ORDER BY category_name";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                combo.addItem(rs.getString("category_name"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStatuses(JComboBox<String> combo) {
        try {
            String sql = "SELECT status_name FROM asset_status ORDER BY status_name";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                combo.addItem(rs.getString("status_name"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadLocations(JComboBox<String> combo) {
        try {
            String sql = "SELECT location_name FROM asset_locations ORDER BY location_name";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                combo.addItem(rs.getString("location_name"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAssets() {
        try {
            tableModel.setRowCount(0);
            String sql = "SELECT * FROM fixed_assets ORDER BY id";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            int totalAssets = 0;
            double totalValue = 0.0;

            while (rs.next()) {
                Asset asset = new Asset(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("cost"),
                        rs.getDate("purchase_date").toLocalDate(),
                        rs.getString("location"),
                        rs.getString("status"),
                        rs.getString("serial_number"),
                        rs.getString("supplier"),
                        rs.getDate("warranty_expiry") != null ?
                                rs.getDate("warranty_expiry").toLocalDate() : null,
                        rs.getDouble("depreciation_rate")
                );
                tableModel.addRow(asset.toTableRow());
                totalAssets++;
                totalValue += asset.getCost();
            }

            // Update status bar
            totalAssetsLabel.setText("Total Assets: " + totalAssets);
            totalValueLabel.setText("Total Value: $" + String.format("%.2f", totalValue));

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading assets: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterTable() {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM fixed_assets WHERE 1=1");
            List<Object> params = new ArrayList<>();

            // Search filter
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                sql.append(" AND (name LIKE ? OR description LIKE ? OR serial_number LIKE ?)");
                String searchPattern = "%" + searchText + "%";
                params.add(searchPattern);
                params.add(searchPattern);
                params.add(searchPattern);
            }

            // Category filter
            String selectedCategory = (String) filterCategoryCombo.getSelectedItem();
            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                sql.append(" AND category = ?");
                params.add(selectedCategory);
            }

            // Status filter
            String selectedStatus = (String) filterStatusCombo.getSelectedItem();
            if (selectedStatus != null && !selectedStatus.equals("All Statuses")) {
                sql.append(" AND status = ?");
                params.add(selectedStatus);
            }

            sql.append(" ORDER BY id");

            PreparedStatement stmt = connection.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            tableModel.setRowCount(0);

            int totalAssets = 0;
            double totalValue = 0.0;

            while (rs.next()) {
                Asset asset = new Asset(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("cost"),
                        rs.getDate("purchase_date").toLocalDate(),
                        rs.getString("location"),
                        rs.getString("status"),
                        rs.getString("serial_number"),
                        rs.getString("supplier"),
                        rs.getDate("warranty_expiry") != null ?
                                rs.getDate("warranty_expiry").toLocalDate() : null,
                        rs.getDouble("depreciation_rate")
                );
                tableModel.addRow(asset.toTableRow());
                totalAssets++;
                totalValue += asset.getCost();
            }

            totalAssetsLabel.setText("Filtered Assets: " + totalAssets);
            totalValueLabel.setText("Filtered Value: $" + String.format("%.2f", totalValue));

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error filtering assets: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFilters() {
        searchField.setText("");
        filterCategoryCombo.setSelectedIndex(0);
        filterStatusCombo.setSelectedIndex(0);
        loadAssets();
    }

    private void showAddAssetDialog() {
        EnhancedAssetDialog dialog = new EnhancedAssetDialog(frame, "Add New Asset", null, connection);
        if (dialog.showDialog()) {
            Asset newAsset = dialog.getAsset();
            if (saveAssetToDatabase(newAsset)) {
                loadAssets();
                JOptionPane.showMessageDialog(frame, "Asset added successfully!");
            }
        }
    }

    private void showEditAssetDialog() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an asset to edit.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int assetId = (Integer) tableModel.getValueAt(selectedRow, 0);
        Asset asset = loadAssetFromDatabase(assetId);

        if (asset != null) {
            EnhancedAssetDialog dialog = new EnhancedAssetDialog(frame, "Edit Asset", asset, connection);
            if (dialog.showDialog()) {
                Asset updatedAsset = dialog.getAsset();
                updatedAsset.id = assetId; // Preserve the ID
                if (updateAssetInDatabase(updatedAsset)) {
                    loadAssets();
                    JOptionPane.showMessageDialog(frame, "Asset updated successfully!");
                }
            }
        }
    }

    private void deleteSelectedAsset() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an asset to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(frame,
                "Are you sure you want to delete this asset?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            int assetId = (Integer) tableModel.getValueAt(selectedRow, 0);
            if (deleteAssetFromDatabase(assetId)) {
                loadAssets();
                JOptionPane.showMessageDialog(frame, "Asset deleted successfully!");
            }
        }
    }

    private void duplicateSelectedAsset() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an asset to duplicate.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int assetId = (Integer) tableModel.getValueAt(selectedRow, 0);
        Asset originalAsset = loadAssetFromDatabase(assetId);

        if (originalAsset != null) {
            // Create a copy with a modified name
            Asset duplicateAsset = new Asset(
                    0, // New ID will be assigned
                    originalAsset.getName() + " (Copy)",
                    originalAsset.getCategory(),
                    originalAsset.getDescription(),
                    originalAsset.getCost(),
                    LocalDate.now(), // Set current date
                    originalAsset.getLocation(),
                    "Active", // Set as active
                    "", // Clear serial number
                    originalAsset.getSupplier(),
                    originalAsset.getWarrantyExpiry(),
                    originalAsset.getDepreciationRate()
            );

            EnhancedAssetDialog dialog = new EnhancedAssetDialog(frame, "Duplicate Asset", duplicateAsset, connection);
            if (dialog.showDialog()) {
                Asset newAsset = dialog.getAsset();
                if (saveAssetToDatabase(newAsset)) {
                    loadAssets();
                    JOptionPane.showMessageDialog(frame, "Asset duplicated successfully!");
                }
            }
        }
    }

    private Asset loadAssetFromDatabase(int assetId) {
        try {
            String sql = "SELECT * FROM fixed_assets WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, assetId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Asset asset = new Asset(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("cost"),
                        rs.getDate("purchase_date").toLocalDate(),
                        rs.getString("location"),
                        rs.getString("status"),
                        rs.getString("serial_number"),
                        rs.getString("supplier"),
                        rs.getDate("warranty_expiry") != null ?
                                rs.getDate("warranty_expiry").toLocalDate() : null,
                        rs.getDouble("depreciation_rate")
                );
                rs.close();
                stmt.close();
                return asset;
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading asset: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private boolean saveAssetToDatabase(Asset asset) {
        try {
            String sql = "INSERT INTO fixed_assets (name, category, description, cost, purchase_date, " +
                    "location, status, serial_number, supplier, warranty_expiry, depreciation_rate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);

            stmt.setString(1, asset.getName());
            stmt.setString(2, asset.getCategory());
            stmt.setString(3, asset.getDescription());
            stmt.setDouble(4, asset.getCost());
            stmt.setDate(5, Date.valueOf(asset.getPurchaseDate()));
            stmt.setString(6, asset.getLocation());
            stmt.setString(7, asset.getStatus());
            stmt.setString(8, asset.getSerialNumber());
            stmt.setString(9, asset.getSupplier());
            stmt.setDate(10, asset.getWarrantyExpiry() != null ?
                    Date.valueOf(asset.getWarrantyExpiry()) : null);
            stmt.setDouble(11, asset.getDepreciationRate());

            int result = stmt.executeUpdate();
            stmt.close();
            return result > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error saving asset: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean updateAssetInDatabase(Asset asset) {
        try {
            String sql = "UPDATE fixed_assets SET name=?, category=?, description=?, cost=?, " +
                    "purchase_date=?, location=?, status=?, serial_number=?, supplier=?, " +
                    "warranty_expiry=?, depreciation_rate=? WHERE id=?";
            PreparedStatement stmt = connection.prepareStatement(sql);

            stmt.setString(1, asset.getName());
            stmt.setString(2, asset.getCategory());
            stmt.setString(3, asset.getDescription());
            stmt.setDouble(4, asset.getCost());
            stmt.setDate(5, Date.valueOf(asset.getPurchaseDate()));
            stmt.setString(6, asset.getLocation());
            stmt.setString(7, asset.getStatus());
            stmt.setString(8, asset.getSerialNumber());
            stmt.setString(9, asset.getSupplier());
            stmt.setDate(10, asset.getWarrantyExpiry() != null ?
                    Date.valueOf(asset.getWarrantyExpiry()) : null);
            stmt.setDouble(11, asset.getDepreciationRate());
            stmt.setInt(12, asset.getId());

            int result = stmt.executeUpdate();
            stmt.close();
            return result > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error updating asset: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean deleteAssetFromDatabase(int assetId) {
        try {
            String sql = "DELETE FROM fixed_assets WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, assetId);

            int result = stmt.executeUpdate();
            stmt.close();
            return result > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error deleting asset: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save CSV File");
        fileChooser.setSelectedFile(new java.io.File("assets_export.csv"));

        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(fileChooser.getSelectedFile())) {
                // Write header
                writer.println("ID,Name,Category,Description,Cost,Purchase Date,Location,Status,Serial Number,Supplier,Warranty Expiry");

                // Write data
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    StringBuilder line = new StringBuilder();
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        if (j > 0) line.append(",");
                        Object value = tableModel.getValueAt(i, j);
                        line.append("\"").append(value != null ? value.toString() : "").append("\"");
                    }
                    writer.println(line.toString());
                }

                JOptionPane.showMessageDialog(frame, "Data exported successfully to " +
                        fileChooser.getSelectedFile().getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Error exporting data: " + e.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAssetSummary() {
        try {
            String sql = "SELECT category, COUNT(*) as count, SUM(cost) as total_value, " +
                    "AVG(cost) as avg_value FROM fixed_assets WHERE status != 'Disposed' " +
                    "GROUP BY category ORDER BY total_value DESC";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            StringBuilder summary = new StringBuilder();
            summary.append("Asset Summary Report\n");
            summary.append("==================\n\n");
            summary.append(String.format("%-20s %8s %12s %12s\n", "Category", "Count", "Total Value", "Avg Value"));
            summary.append("--------------------------------------------------------\n");

            while (rs.next()) {
                summary.append(String.format("%-20s %8d $%11.2f $%11.2f\n",
                        rs.getString("category"),
                        rs.getInt("count"),
                        rs.getDouble("total_value"),
                        rs.getDouble("avg_value")));
            }

            JTextArea textArea = new JTextArea(summary.toString());
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setEditable(false);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 300));

            JOptionPane.showMessageDialog(frame, scrollPane, "Asset Summary", JOptionPane.INFORMATION_MESSAGE);

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error generating summary: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDepreciationReport() {
        try {
            String sql = "SELECT name, category, cost, purchase_date, depreciation_rate, " +
                    "ROUND(cost * (1 - (depreciation_rate / 100) * " +
                    "DATEDIFF(CURDATE(), purchase_date) / 365), 2) as current_value " +
                    "FROM fixed_assets WHERE status != 'Disposed' AND depreciation_rate > 0 " +
                    "ORDER BY category, name";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            StringBuilder report = new StringBuilder();
            report.append("Depreciation Report\n");
            report.append("==================\n\n");
            report.append(String.format("%-25s %-15s %12s %12s %8s %12s\n",
                    "Asset Name", "Category", "Original", "Current", "Rate%", "Purchase Date"));
            report.append("---------------------------------------------------------------------------------\n");

            while (rs.next()) {
                report.append(String.format("%-25s %-15s $%11.2f $%11.2f %7.1f%% %12s\n",
                        rs.getString("name").length() > 25 ?
                                rs.getString("name").substring(0, 22) + "..." : rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("cost"),
                        rs.getDouble("current_value"),
                        rs.getDouble("depreciation_rate"),
                        rs.getDate("purchase_date").toString()));
            }

            JTextArea textArea = new JTextArea(report.toString());
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setEditable(false);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(700, 400));

            JOptionPane.showMessageDialog(frame, scrollPane, "Depreciation Report", JOptionPane.INFORMATION_MESSAGE);

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error generating depreciation report: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void show() {
        frame.setVisible(true);
    }

    public void dispose() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        frame.dispose();
    }

    // Enhanced Dialog class for adding/editing assets with dropdowns
    static class EnhancedAssetDialog extends JDialog {
        private JTextField nameField, descriptionField, costField, purchaseDateField,
                serialNumberField, supplierField, warrantyExpiryField, depreciationRateField;
        private JComboBox<String> categoryCombo, locationCombo, statusCombo;
        private boolean confirmed = false;
        private Asset asset;
        private Connection connection;

        public EnhancedAssetDialog(JFrame parent, String title, Asset existingAsset, Connection conn) {
            super(parent, title, true);
            this.asset = existingAsset;
            this.connection = conn;
            initializeDialog();
        }

        private void initializeDialog() {
            setSize(500, 500);
            setLocationRelativeTo(getParent());
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            // Create form fields
            nameField = new JTextField(20);
            categoryCombo = new JComboBox<>();
            loadCategories(categoryCombo);
            descriptionField = new JTextField(20);
            costField = new JTextField(20);
            purchaseDateField = new JTextField(20);
            locationCombo = new JComboBox<>();
            loadLocations(locationCombo);
            statusCombo = new JComboBox<>();
            loadStatuses(statusCombo);
            serialNumberField = new JTextField(20);
            supplierField = new JTextField(20);
            warrantyExpiryField = new JTextField(20);
            depreciationRateField = new JTextField(20);

            // Pre-populate if editing
            if (asset != null) {
                nameField.setText(asset.getName());
                categoryCombo.setSelectedItem(asset.getCategory());
                descriptionField.setText(asset.getDescription());
                costField.setText(String.valueOf(asset.getCost()));
                purchaseDateField.setText(asset.getPurchaseDate().toString());
                locationCombo.setSelectedItem(asset.getLocation());
                statusCombo.setSelectedItem(asset.getStatus());
                serialNumberField.setText(asset.getSerialNumber() != null ? asset.getSerialNumber() : "");
                supplierField.setText(asset.getSupplier() != null ? asset.getSupplier() : "");
                warrantyExpiryField.setText(asset.getWarrantyExpiry() != null ?
                        asset.getWarrantyExpiry().toString() : "");
                depreciationRateField.setText(String.valueOf(asset.getDepreciationRate()));
            } else {
                // Default values for new asset
                purchaseDateField.setText(LocalDate.now().toString());
                statusCombo.setSelectedItem("Active");
                depreciationRateField.setText("0.0");
            }

            // Add components to dialog
            int row = 0;

            gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
            add(new JLabel("Name *:"), gbc);
            gbc.gridx = 1; add(nameField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Category *:"), gbc);
            gbc.gridx = 1; add(categoryCombo, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Description:"), gbc);
            gbc.gridx = 1; add(descriptionField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Cost *:"), gbc);
            gbc.gridx = 1; add(costField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Purchase Date * (YYYY-MM-DD):"), gbc);
            gbc.gridx = 1; add(purchaseDateField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Location *:"), gbc);
            gbc.gridx = 1; add(locationCombo, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Status *:"), gbc);
            gbc.gridx = 1; add(statusCombo, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Serial Number:"), gbc);
            gbc.gridx = 1; add(serialNumberField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Supplier:"), gbc);
            gbc.gridx = 1; add(supplierField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Warranty Expiry (YYYY-MM-DD):"), gbc);
            gbc.gridx = 1; add(warrantyExpiryField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row;
            add(new JLabel("Depreciation Rate (%):"), gbc);
            gbc.gridx = 1; add(depreciationRateField, gbc);

            // Button panel
            JPanel buttonPanel = new JPanel();
            JButton okButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");

            okButton.addActionListener(e -> {
                if (validateAndSave()) {
                    confirmed = true;
                    dispose();
                }
            });

            cancelButton.addActionListener(e -> dispose());

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(buttonPanel, gbc);
        }

        private void loadCategories(JComboBox<String> combo) {
            try {
                String sql = "SELECT category_name FROM asset_categories ORDER BY category_name";
                PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    combo.addItem(rs.getString("category_name"));
                }

                rs.close();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void loadLocations(JComboBox<String> combo) {
            try {
                String sql = "SELECT location_name FROM asset_locations ORDER BY location_name";
                PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    combo.addItem(rs.getString("location_name"));
                }

                rs.close();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void loadStatuses(JComboBox<String> combo) {
            try {
                String sql = "SELECT status_name FROM asset_status ORDER BY status_name";
                PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    combo.addItem(rs.getString("status_name"));
                }

                rs.close();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private boolean validateAndSave() {
            try {
                String name = nameField.getText().trim();
                String category = (String) categoryCombo.getSelectedItem();
                String description = descriptionField.getText().trim();
                double cost = Double.parseDouble(costField.getText().trim());
                LocalDate purchaseDate = LocalDate.parse(purchaseDateField.getText().trim());
                String location = (String) locationCombo.getSelectedItem();
                String status = (String) statusCombo.getSelectedItem();
                String serialNumber = serialNumberField.getText().trim();
                String supplier = supplierField.getText().trim();
                String warrantyExpiryText = warrantyExpiryField.getText().trim();
                LocalDate warrantyExpiry = warrantyExpiryText.isEmpty() ? null : LocalDate.parse(warrantyExpiryText);
                double depreciationRate = Double.parseDouble(depreciationRateField.getText().trim());

                if (name.isEmpty() || category == null || location == null || status == null) {
                    JOptionPane.showMessageDialog(this, "Please fill in all required fields (marked with *).",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                if (cost < 0) {
                    JOptionPane.showMessageDialog(this, "Cost must be a positive number.",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                if (depreciationRate < 0 || depreciationRate > 100) {
                    JOptionPane.showMessageDialog(this, "Depreciation rate must be between 0 and 100.",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                asset = new Asset(0, name, category, description, cost, purchaseDate, location, status,
                        serialNumber, supplier, warrantyExpiry, depreciationRate);
                return true;

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for cost and depreciation rate.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return false;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please enter valid dates in YYYY-MM-DD format.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        public boolean showDialog() {
            setVisible(true);
            return confirmed;
        }

        public Asset getAsset() {
            return asset;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new FixedAssetsRegistry().show();
        });
    }
}
