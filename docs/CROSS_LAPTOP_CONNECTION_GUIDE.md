# Cross-Laptop Database Connection Guide (Student 1 <-> Student 2)

This guide provides the exact step-by-step instructions to connect your **Frontend / Java Application Host (Your Laptop)** to the **Backend / Database Host (Your Friend's Laptop)** using your friend's IP address.

---

## Architecture Overview

```
+------------------------------------+                +------------------------------------+
|       YOUR LAPTOP (Student 1)      |                |     FRIEND'S LAPTOP (Student 2)    |
|   Frontend / Application Host      |                |      Backend / Database Host       |
|                                    |                |                                    |
|   - Subsea Web Dashboard           |                |   - MySQL Server 8.0               |
|     (http://localhost:8080)        |  Port 3306     |   - Database: subsea_db or         |
|   - Telemetry Ingestion Engine     |───────────────>|               subsea_pipeline      |
|   - Target: FRIEND_IP:3306         |  (Wi-Fi / LAN) |   - Listening on 0.0.0.0:3306      |
|                                    |                |   - IP: e.g. 192.168.1.50          |
+------------------------------------+                +------------------------------------+
```

---

## PART 1: What Your Friend Must Do on Their Laptop (Backend Host)

Your friend's laptop acts as the database server. Follow these 4 quick steps on your friend's laptop:

### Step 1: Connect Both Laptops to the Same Network
* Ensure **both laptops** are connected to the **same Wi-Fi router** or **mobile hotspot**.

### Step 2: Find Your Friend's IPv4 Address
1. On your friend's laptop, open **PowerShell** or **Command Prompt**.
2. Type:
   ```cmd
   ipconfig
   ```
3. Locate the **IPv4 Address** under the active Wi-Fi or Ethernet adapter (for example: `192.168.1.50`).
4. **Note down this IP address** — you will use it on your laptop.

### Step 3: Open MySQL Port 3306 in Windows Firewall
By default, Windows Firewall blocks incoming connections to MySQL. Your friend must allow port 3306:
1. On your friend's laptop, open **PowerShell as Administrator**.
2. Run this command:
   ```powershell
   New-NetFirewallRule -DisplayName "MySQL Remote Port 3306" -Direction Inbound -LocalPort 3306 -Protocol TCP -Action Allow
   ```

### Step 4: Grant Remote User Permissions in MySQL
Your friend must allow MySQL connections from other laptops:
1. Open **MySQL Workbench** on your friend's laptop and connect as `root`.
2. Run the following SQL script:
   ```sql
   -- 1. Create or update user for Student 2 pipeline (subsea_db)
   CREATE USER IF NOT EXISTS 'subsea_user'@'%' IDENTIFIED BY 'SubseaSecurePass2026!';
   GRANT ALL PRIVILEGES ON subsea_db.* TO 'subsea_user'@'%';

   -- 2. Create or update user for Student 1 pipeline (subsea_pipeline)
   CREATE USER IF NOT EXISTS 'subsea_app'@'%' IDENTIFIED BY 'SubseaApp@2026#ChangeMe';
   GRANT ALL PRIVILEGES ON subsea_pipeline.* TO 'subsea_app'@'%';

   -- 3. Flush privileges to apply changes immediately
   FLUSH PRIVILEGES;
   ```
3. *(Optional check)*: Ensure MySQL is bound to all network interfaces:
   Open `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`. Ensure `bind-address=0.0.0.0` (or comment out `bind-address=127.0.0.1`), then restart the MySQL service.

---

## PART 2: What You Do on Your Laptop (Frontend / App Host)

Now on your laptop, configure the friend's IP and launch the system:

### Option A: Using the 1-Click Interactive Batch Script (Easiest)

1. Open PowerShell or Command Prompt in your project folder:
   ```cmd
   cd C:\Users\pavit\Documents\SubseaPipelineIntegrityMonitoringSystem
   ```
2. Run:
   ```cmd
   test_remote_db.bat <YOUR_FRIEND_IP>
   ```
   *(Example: `test_remote_db.bat 192.168.1.50`)*

The script will automatically:
* Test TCP port 3306 connectivity to your friend's laptop.
* Update `config/application.properties` with your friend's IP address.
* Prompt you to start the **Web Dashboard Server (`http://localhost:8080`)** or run the monitoring engine!

---

### Option B: Manual Configuration

If you prefer configuring manually:

1. Open `config/application.properties` in VS Code or Notepad:
   ```properties
   db.host=192.168.1.50     # <--- Replace with your friend's IP
   db.port=3306
   db.name=subsea_db
   db.user=subsea_user
   db.password=SubseaSecurePass2026!
   ```
2. Compile and run:
   ```cmd
   compile.bat
   run.bat server
   ```
3. Open your browser and navigate to:
   ```
   http://localhost:8080
   ```
   The dashboard will now fetch live telemetry data directly from your friend's laptop MySQL database!

---

## PART 3: Quick Troubleshooting

| Error Message / Symptom | Cause | Solution |
| :--- | :--- | :--- |
| `Port 3306 did not respond / Connection timed out` | Windows Firewall on friend's laptop is blocking port 3306. | On friend's laptop, run PowerShell as Administrator and execute the `New-NetFirewallRule` command from Part 1 Step 3. |
| `Access denied for user 'subsea_user'@'...'` | MySQL user does not have permission from remote IP. | In MySQL Workbench on friend's laptop, run `CREATE USER 'subsea_user'@'%' IDENTIFIED BY 'SubseaSecurePass2026!'; GRANT ALL PRIVILEGES ON *.* TO 'subsea_user'@'%'; FLUSH PRIVILEGES;`. |
| `Unknown database 'subsea_db'` | Database has not been created yet on friend's laptop. | In MySQL Workbench on friend's laptop, run `database/subsea_pipeline_schema.sql` to instantiate the schema and tables. |
| `Laptops cannot reach each other` | Laptops are on different networks or guest isolation is enabled. | Connect both laptops to a Mobile Hotspot from one phone (disable AP isolation) or verify ping with `ping <FRIEND_IP>`. |
