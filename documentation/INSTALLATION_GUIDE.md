# Installation and Setup Guide - Windows & Eclipse IDE

This guide provides step-by-step instructions for setting up, compiling, and running the Subsea Pipeline Integrity Monitoring System on a Windows laptop using standard developer tools.

---

## 1. Prerequisites and Verification

Before running the project, verify that the following software is installed on your Windows machine:

### A. Java Development Kit (JDK 21 LTS)
Open PowerShell or Command Prompt and run:
```powershell
javac -version
java -version
```
*Expected output*: `javac 21.x.x` and `java version "21.x.x"`

### B. Apache Maven (3.9+)
Verify Maven installation:
```powershell
mvn -v
```
*Note*: If Maven was just installed to `C:\Users\<Username>\tools\apache-maven-3.9.6\bin`, ensure that this path is in your User `PATH` environment variable.

### C. MySQL Server 8.0 & MySQL Workbench
Verify MySQL service is running:
```powershell
Get-Service -Name *mysql*
```
*Expected status*: `Running` (Service name `MySQL80`).

---

## 2. Opening the Project in Eclipse IDE

### Step 1: Open Eclipse
* Launch **Eclipse IDE for Java Developers** (from Desktop shortcut or Start Menu).
* Select your preferred workspace directory (e.g. `C:\Users\<Username>\eclipse-workspace`).

### Step 2: Import as Existing Maven Project
1. Go to top menu: **File -> Import...**
2. In the Import wizard, expand the **Maven** folder.
3. Select **Existing Maven Projects** and click **Next**.
4. In the **Root Directory** field, click **Browse...** and select:
   `C:\Users\<Username>\Documents\SubseaPipelineIntegrityMonitoringSystem`
5. Eclipse will automatically detect `/pom.xml`. Ensure the checkbox next to `pom.xml` is checked.
6. Click **Finish**.

### Step 3: Update Maven Dependencies in Eclipse
1. In the **Project Explorer** view, right-click on `SubseaPipelineIntegrityMonitoringSystem`.
2. Select **Maven -> Update Project...** (or press `Alt + F5`).
3. Check **Force Update of Snapshots/Releases**.
4. Click **OK**. Eclipse will configure the Maven Classpath Container and download any needed libraries.

---

## 3. Running the Project in Eclipse IDE

### Run the Main Application
1. In **Project Explorer**, expand:
   `src/main/java` -> `com.subsea`
2. Right-click on **`MainApp.java`**.
3. Select **Run As -> Java Application**.
4. The **Console** tab at the bottom of Eclipse will display:
   * System Banner and Role definitions.
   * Connection verification to MySQL (`subsea_pipeline`).
   * Stage 1: Multithreaded telemetry acquisition from PIG inspections and corrosion audits.
   * Stage 2: Concurrent calculation of PCI scores, rupture risks, and hazard codes.
   * Stage 3: Database insertion into `repair_orders`.
   * Stage 4: Formatted ASCII assessment summary table.
   * Stage 5: CSV report generation in `reports/`.

### Run Unit and Integration Tests in Eclipse
1. In **Project Explorer**, expand:
   `src/test/java` -> `com.subsea`
2. Right-click on the package `com.subsea` (or any individual test class like `CalculationTest.java` or `PipelineServiceTest.java`).
3. Select **Run As -> JUnit Test**.
4. The **JUnit** view will show a green bar indicating **7/7 tests passed**.

---

## 4. Running the Project from Windows PowerShell / Terminal

If you prefer using the command line:

### Step 1: Navigate to the Project Root
```powershell
cd C:\Users\<Username>\Documents\SubseaPipelineIntegrityMonitoringSystem
```

### Step 2: Compile and Run Tests
```powershell
mvn clean test
```

### Step 3: Package into an Executable Fat JAR
```powershell
mvn clean package -DskipTests
```

### Step 4: Execute the JAR
```powershell
java -jar target\subsea-pipeline-monitoring-1.0.0-all.jar
```

---

## 5. Troubleshooting Common Issues

### Issue 1: "Access denied for user 'subsea_app'@'localhost'"
* **Cause**: Database user permissions not applied or password mismatch.
* **Resolution**: Open MySQL Workbench as `root`, open `database/subsea_pipeline.sql`, and execute Section 6 to recreate user `subsea_app`:
  ```sql
  CREATE USER IF NOT EXISTS 'subsea_app'@'%' IDENTIFIED BY 'SubseaApp@2026#ChangeMe';
  GRANT SELECT, INSERT, UPDATE, DELETE ON subsea_pipeline.* TO 'subsea_app'@'%';
  FLUSH PRIVILEGES;
  ```

### Issue 2: "Communications link failure (Connection refused)"
* **Cause**: MySQL Server service is stopped.
* **Resolution**: Start the service in PowerShell (Run as Administrator):
  ```powershell
  Start-Service -Name MySQL80
  ```

### Issue 3: Eclipse shows red exclamation mark on project
* **Cause**: JRE version mismatch in Eclipse.
* **Resolution**: Right-click project -> **Properties -> Java Build Path -> Libraries** -> Select **JRE System Library** -> Click **Edit...** -> Select **Workspace default JRE (jdk-21)** -> Click **Apply and Close**.
