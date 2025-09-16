# Fixed Assets Registry

A comprehensive Java Swing desktop application for managing organizational fixed assets with MySQL database integration.

## Overview

The Fixed Assets Registry is a professional desktop application designed to help organizations track, manage, and maintain records of their fixed assets. The application provides a user-friendly interface for asset lifecycle management, from acquisition to disposal, with integrated reporting and depreciation tracking capabilities.

## Features

### Core Asset Management
- **Add, Edit, and Delete Assets**: Complete CRUD operations for asset records
- **Asset Duplication**: Clone existing assets for quick entry of similar items
- **Comprehensive Asset Details**: Track name, category, description, cost, purchase date, location, status, serial number, supplier, warranty information, and depreciation rates

### Search and Filtering
- **Dynamic Search**: Real-time search across asset names, descriptions, and serial numbers
- **Category Filtering**: Filter assets by predefined categories
- **Status Filtering**: Filter by asset status (Active, Inactive, Disposed, etc.)
- **Multi-criteria Filtering**: Combine search terms with category and status filters

### Database Integration
- **MySQL Backend**: Robust MySQL database storage with proper relationship management
- **Normalized Schema**: Separate tables for categories, locations, and statuses for data integrity
- **Transaction Safety**: Proper error handling and database transaction management

### Reporting and Analytics
- **Asset Summary Report**: Categorized breakdown of asset counts, total values, and averages
- **Depreciation Report**: Calculated current values based on depreciation rates and time
- **CSV Export**: Export filtered or complete asset data to CSV format
- **Real-time Statistics**: Live display of total asset count and cumulative value

### User Interface
- **Professional Design**: Clean, intuitive Swing-based GUI
- **Sortable Tables**: Click column headers to sort data
- **Responsive Layout**: Properly sized and organized interface elements
- **Data Validation**: Input validation with user-friendly error messages

## Technical Specifications

### System Requirements
- Java 8 or higher
- MySQL 5.7 or higher
- MySQL Connector/J JDBC driver
- Minimum 512 MB RAM
- 50 MB disk space

### Technology Stack
- **Frontend**: Java Swing
- **Backend**: MySQL Database
- **Database Connectivity**: JDBC with MySQL Connector/J
- **Architecture**: Model-View-Controller (MVC) pattern

## Installation

### Prerequisites
1. Install Java Development Kit (JDK) 8 or higher
2. Install MySQL Server
3. Download MySQL Connector/J JDBC driver

### Database Setup
1. Create a MySQL database named `asset_registry_db`
2. Execute the following SQL to create required tables:

```sql
-- Main assets table
CREATE TABLE fixed_assets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    cost DECIMAL(12,2) NOT NULL,
    purchase_date DATE NOT NULL,
    location VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    serial_number VARCHAR(100),
    supplier VARCHAR(255),
    warranty_expiry DATE,
    depreciation_rate DECIMAL(5,2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Categories lookup table
CREATE TABLE asset_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) UNIQUE NOT NULL
);

-- Locations lookup table
CREATE TABLE asset_locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    location_name VARCHAR(100) UNIQUE NOT NULL
);

-- Status lookup table
CREATE TABLE asset_status (
    id INT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(50) UNIQUE NOT NULL
);

-- Insert default categories
INSERT INTO asset_categories (category_name) VALUES 
('Computer Equipment'), ('Office Furniture'), ('Vehicles'), 
('Machinery'), ('Software'), ('Building & Infrastructure');

-- Insert default locations
INSERT INTO asset_locations (location_name) VALUES 
('Main Office'), ('Warehouse'), ('Branch Office A'), 
('Branch Office B'), ('Remote Location');

-- Insert default statuses
INSERT INTO asset_status (status_name) VALUES 
('Active'), ('Inactive'), ('Under Maintenance'), 
('Disposed'), ('Lost/Stolen');
```

### Application Setup
1. Clone or download the project files
2. Update database connection settings in `FixedAssetsRegistry.java`:
   ```java
   private static final String DB_URL = "jdbc:mysql://localhost:3306/asset_registry_db";
   private static final String DB_USER = "your_username";
   private static final String DB_PASSWORD = "your_password";
   ```
3. Compile the Java application:
   ```bash
   javac -cp ".:mysql-connector-java.jar" FixedAssetsRegistry.java
   ```
4. Run the application:
   ```bash
   java -cp ".:mysql-connector-java.jar" FixedAssetsRegistry
   ```

## Usage

### Adding Assets
1. Click the "Add Asset" button
2. Fill in the required fields (marked with *)
3. Select appropriate category, location, and status from dropdown menus
4. Enter optional fields like serial number, supplier, and warranty information
5. Click "Save" to add the asset to the database

### Searching and Filtering
1. Use the search field to find assets by name, description, or serial number
2. Select specific categories or statuses from the dropdown filters
3. Use "Clear Filters" to reset all search criteria
4. The status bar shows the count and total value of filtered results

### Generating Reports
1. Access reports through the "Reports" menu
2. **Asset Summary**: Shows breakdown by category with counts and values
3. **Depreciation Report**: Displays calculated current values based on depreciation

### Exporting Data
1. Use "File" → "Export to CSV" to save current table data
2. Choose the destination file location
3. The export includes all visible (filtered) records

## Security Considerations

### Database Security
- Change default database credentials before deployment
- Use strong passwords for database user accounts
- Implement proper database user permissions (avoid using root account)
- Consider database encryption for sensitive data

### Application Security
- The application currently stores database credentials in source code
- For production use, consider external configuration files or environment variables
- Implement user authentication and authorization as needed
- Consider data encryption for sensitive asset information

## Customization

### Adding New Categories, Locations, or Statuses
Add entries directly to the respective database tables:
```sql
INSERT INTO asset_categories (category_name) VALUES ('New Category');
INSERT INTO asset_locations (location_name) VALUES ('New Location');
INSERT INTO asset_status (status_name) VALUES ('New Status');
```

### Modifying Asset Fields
To add new asset properties:
1. Add columns to the `fixed_assets` table
2. Update the `Asset` class with new fields and getters
3. Modify the `EnhancedAssetDialog` to include new input fields
4. Update database insert/update methods

## Troubleshooting

### Common Issues

**Database Connection Failure**
- Verify MySQL server is running
- Check database credentials and connection URL
- Ensure MySQL Connector/J is in the classpath
- Verify database and tables exist

**Application Won't Start**
- Check Java version compatibility
- Verify all required JAR files are present
- Check for missing database tables
- Review console output for specific error messages

**Data Not Displaying**
- Verify database contains data
- Check for SQL syntax errors in console output
- Ensure table structure matches expected schema

## Contributing

To contribute to this project:
1. Fork the repository
2. Create a feature branch
3. Make changes with proper testing
4. Submit a pull request with detailed description

## License

This project is provided as-is for educational and commercial use. Modify and distribute according to your organization's requirements.

## Support

For technical support or feature requests, please review the code documentation and database schema. The application includes comprehensive error handling with descriptive messages to assist with troubleshooting.