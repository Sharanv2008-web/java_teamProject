# Oracle Cloud Infrastructure (OCI) Deployment Guide

This guide describes the complete procedure for deploying the **Subsea Pipeline Integrity Monitoring System** across two virtual machines within Oracle Cloud Infrastructure (OCI) using a secure, private network architecture.

---

## 1. Cloud Architecture Overview

To maintain industry-standard security and protect critical offshore pipeline infrastructure:
* **OCI VM 1 (Java Application Host - Student 1 Responsibility)**: Runs the compiled Java monitoring application, executes multithreaded calculation cycles, and writes CSV reports.
* **OCI VM 2 (Database Host - Student 2 Responsibility)**: Runs MySQL Server 8.0 on a private subnet.
* **Network Isolation**: MySQL port `3306` is **never exposed to the public internet**. VM 1 connects to VM 2 exclusively through an internal OCI Virtual Cloud Network (VCN) private IP address.

```
                  +----------------------------------------------+
                  |           Oracle Cloud Infrastructure        |
                  |             Virtual Cloud Network (VCN)      |
                  +----------------------------------------------+
                                         │
        ┌────────────────────────────────┴────────────────────────────────┐
        │                                                                 │
        ▼                                                                 ▼
+-----------------------------------+             +-----------------------------------+
|     Public Subnet (10.0.0.0/24)   |             |    Private Subnet (10.0.1.0/24)   |
|                                   |             |                                   |
|   OCI VM 1: Java Application Host |   Private   |      OCI VM 2: Database Host      |
|   Private IP: 10.0.0.15           |────────────>|      Private IP: 10.0.1.20        |
|   Public IP:  138.2.x.x (SSH only)|  Port 3306  |      Public IP:  NONE             |
|                                   |             |                                   |
|   * OpenJDK 21 LTS                |             |   * MySQL 8.0 Server              |
|   * Executable Application JAR    |             |   * Database: 'subsea_pipeline'   |
|   * Environment Config            |             |   * User: 'subsea_app'@'10.0.0.%' |
+-----------------------------------+             +-----------------------------------+
        ▲
        │ SSH (Port 22 only)
        │
┌───────┴───────┐
│ Administrator │
└───────────────┘
```

---

## 2. OCI Network Security List & Firewall Rules

### VM 1 (Public Subnet) Ingress Security Rules
* **Protocol**: TCP
* **Port**: `22` (SSH for administrative management)
* **Source CIDR**: Your individual authorized IP address or corporate bastion CIDR (e.g., `YOUR_OFFICE_IP/32`).

### VM 2 (Private Subnet) Ingress Security Rules (Strictly Restricted)
* **Rule 1 (Internal MySQL Traffic)**:
  * **Protocol**: TCP
  * **Destination Port**: `3306`
  * **Source CIDR**: `10.0.0.0/24` (Traffic permitted exclusively from VM 1's subnet).
  * **Public Internet (0.0.0.0/0)**: **STRICTLY PROHIBITED**.
* **Rule 2 (Internal SSH Management from VM 1)**:
  * **Protocol**: TCP
  * **Destination Port**: `22`
  * **Source CIDR**: `10.0.0.15/32` (Only VM 1 can SSH into VM 2).

---

## 3. Student 1: VM 1 (Java Application Host) Setup Instructions

### Step 1: Provision VM 1
* **Image**: Ubuntu 22.04 LTS or Canonical Ubuntu 24.04
* **Shape**: `VM.Standard.A1.Flex` (Always Free 2 OCPU, 12GB RAM) or `VM.Standard.E2.1.Micro`.
* **VCN**: Attach to the Public Subnet (assign public IPv4).

### Step 2: Connect via SSH
```bash
ssh -i ~/.ssh/id_rsa ubuntu@<VM1_PUBLIC_IP>
```

### Step 3: Install Java 21 LTS and Essential Packages
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-21-jdk openjdk-21-jre-headless git curl ufw
java -version
```

### Step 4: Transfer Application Artifacts to VM 1
From your Windows laptop, upload the standalone fat JAR and config file:
```powershell
scp -i ~/.ssh/id_rsa `
  target/subsea-pipeline-monitoring-1.0.0-all.jar `
  ubuntu@<VM1_PUBLIC_IP>:/home/ubuntu/
```

Create application directories on VM 1:
```bash
mkdir -p /home/ubuntu/subsea-app/reports
mkdir -p /home/ubuntu/subsea-app/logs
mkdir -p /home/ubuntu/subsea-app/config
mv /home/ubuntu/subsea-pipeline-monitoring-1.0.0-all.jar /home/ubuntu/subsea-app/
```

### Step 5: Configure Runtime Environment Variables
Create an environment file `/home/ubuntu/subsea-app/.env`:
```bash
cat << 'EOF' > /home/ubuntu/subsea-app/.env
# Database Host Configuration (Pointing to Student 2's Private IP)
export DB_HOST="10.0.1.20"
export DB_PORT="3306"
export DB_NAME="subsea_pipeline"
export DB_USER="subsea_app"
export DB_PASSWORD="SubseaApp@2026#ChangeMe"
EOF
chmod 600 /home/ubuntu/subsea-app/.env
```

### Step 6: Test Database Connectivity from VM 1 to VM 2
Before launching the application, verify that VM 1 can communicate with VM 2 over the private network:
```bash
# Test TCP reachability to port 3306 on VM 2
nc -zv 10.0.1.20 3306
```
*Expected output*: `Connection to 10.0.1.20 3306 port [tcp/mysql] succeeded!`

### Step 7: Manual Execution Test
```bash
cd /home/ubuntu/subsea-app
source .env
java -jar subsea-pipeline-monitoring-1.0.0-all.jar
```
Verify that the output displays the ASCII assessment table and that CSV reports are written to `/home/ubuntu/subsea-app/reports/`.

---

## 4. Production Daemon Configuration (Systemd Service)

To run the application continuously or on a scheduled basis without requiring an active terminal session:

### Step 1: Create Systemd Service File
```bash
sudo bash -c 'cat << "EOF" > /etc/systemd/system/subsea-monitor.service
[Unit]
Description=Subsea Pipeline Integrity Monitoring Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/subsea-app
EnvironmentFile=/home/ubuntu/subsea-app/.env
ExecStart=/usr/bin/java -jar /home/ubuntu/subsea-app/subsea-pipeline-monitoring-1.0.0-all.jar
Restart=on-failure
RestartSec=30
StandardOutput=append:/home/ubuntu/subsea-app/logs/application.log
StandardError=append:/home/ubuntu/subsea-app/logs/application.log

[Install]
WantedBy=multi-user.target
EOF'
```

### Step 2: Enable and Start Service
```bash
sudo systemctl daemon-reload
sudo systemctl enable subsea-monitor.service
sudo systemctl start subsea-monitor.service
sudo systemctl status subsea-monitor.service
```

### Step 3: Inspect Real-Time Application Logs
```bash
tail -f /home/ubuntu/subsea-app/logs/application.log
```

---

## 5. Deployment Troubleshooting Reference

| Symptom | Probable Cause | Verification & Resolution |
| :--- | :--- | :--- |
| `nc -zv 10.0.1.20 3306` hangs or times out | OCI Security List does not permit ingress on port 3306 from `10.0.0.0/24`. | Check OCI Console -> Virtual Cloud Networks -> Private Subnet Security List -> Add Ingress Rule for CIDR `10.0.0.0/24`, TCP Port `3306`. |
| `Access denied for user 'subsea_app'@'10.0.0.15'` | MySQL user host wildcard `%` or subnet CIDR not authorized. | On VM 2, verify user host: `SELECT user, host FROM mysql.user WHERE user='subsea_app';`. If needed: `CREATE USER 'subsea_app'@'10.0.0.%' IDENTIFIED BY '...'; GRANT ALL ON subsea_pipeline.* TO 'subsea_app'@'10.0.0.%'; FLUSH PRIVILEGES;` |
| `Communications link failure / Connection refused` | MySQL is bound to `127.0.0.1` only. | On VM 2, inspect `/etc/mysql/mysql.conf.d/mysqld.cnf`. Ensure `bind-address = 0.0.0.0`, then restart MySQL: `sudo systemctl restart mysql`. |
