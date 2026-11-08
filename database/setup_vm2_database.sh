#!/usr/bin/env bash
# =============================================================================
# Subsea Pipeline Integrity Diagnostics System - Student 2 (Database Host)
# OCI VM 2 Database Automated Provisioning & Remote Access Setup Script
# Target OS: Oracle Linux 8/9 / Ubuntu 22.04 LTS on OCI VM 2
# Port: 3306 (MySQL)
# =============================================================================

set -e

echo "======================================================================"
echo " [STUDENT 2] Initiating Subsea DB Host Deployment on OCI VM 2"
echo " Timestamp: $(date)"
echo " Hostname:  $(hostname)"
echo " IP Addr:   $(hostname -I | awk '{print $1}')"
echo "======================================================================"

DB_NAME="subsea_db"
DB_USER="subsea_user"
DB_PASS="SubseaSecurePass2026!"
DB_PORT=3306

# 1. Detect OS & Install MySQL Server
if [ -f /etc/oracle-release ] || [ -f /etc/redhat-release ]; then
    echo "[1/6] Detected Oracle Linux / RHEL environment. Installing MySQL Server..."
    sudo dnf install -y mysql-server
    sudo systemctl enable mysqld
    sudo systemctl start mysqld
    MYSQL_CONF="/etc/my.cnf.d/mysql-server.cnf"
    
    # Configure bind-address
    if grep -q "bind-address" "$MYSQL_CONF" 2>/dev/null; then
        sudo sed -i 's/^bind-address.*/bind-address=0.0.0.0/' "$MYSQL_CONF"
    else
        echo -e "\n[mysqld]\nbind-address=0.0.0.0\nport=3306\n" | sudo tee -a "$MYSQL_CONF"
    fi

    # Configure firewalld
    echo "[2/6] Configuring OS Firewall (firewalld)..."
    sudo firewall-cmd --zone=public --add-port=${DB_PORT}/tcp --permanent || true
    sudo firewall-cmd --reload || true
else
    echo "[1/6] Detected Debian/Ubuntu environment. Installing MySQL Server..."
    sudo apt-get update -y
    sudo apt-get install -y mysql-server
    sudo systemctl enable mysql
    sudo systemctl start mysql
    MYSQL_CONF="/etc/mysql/mysql.conf.d/mysqld.cnf"

    # Configure bind-address
    sudo sed -i 's/127.0.0.1/0.0.0.0/g' "$MYSQL_CONF"

    # Configure UFW firewall
    echo "[2/6] Configuring OS Firewall (UFW)..."
    sudo ufw allow ${DB_PORT}/tcp || true
fi

# 2. Configure Linux iptables rules (OCI Oracle Linux default enforces REJECT in iptables)
echo "[3/6] Ensuring iptables allows remote incoming traffic on port ${DB_PORT}..."
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport ${DB_PORT} -j ACCEPT || sudo iptables -I INPUT 1 -p tcp --dport ${DB_PORT} -j ACCEPT

# Save iptables rules
if command -v netfilter-persistent &> /dev/null; then
    sudo netfilter-persistent save
elif [ -f /etc/oracle-release ]; then
    sudo service iptables save || true
fi

# 3. Restart MySQL to bind to 0.0.0.0
echo "[4/6] Restarting MySQL service with remote bind-address 0.0.0.0..."
if [ -f /etc/oracle-release ]; then
    sudo systemctl restart mysqld
else
    sudo systemctl restart mysql
fi

# 4. Provision Subsea Database, Remote User and Permissions
echo "[5/6] Creating database '${DB_NAME}' and granting remote access to '${DB_USER}'@'%'..."
sudo mysql -uroot <<EOF
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create remote access user for Student 1 (Application Host on VM 1)
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'%';

-- Create local access user
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';

FLUSH PRIVILEGES;
EOF

# 5. Ingest Schema & Seed Data
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA_FILE="${SCRIPT_DIR}/subsea_pipeline_schema.sql"

if [ -f "$SCHEMA_FILE" ]; then
    echo "[6/6] Executing schema DDL and Seed Data from ${SCHEMA_FILE}..."
    sudo mysql -u"${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "$SCHEMA_FILE"
    echo ">> Database Schema and seed data successfully initialized!"
else
    echo "[6/6] Note: ${SCHEMA_FILE} not found in script dir. Please load schema manually."
fi

echo "======================================================================"
echo " [STUDENT 2] VM 2 Database Host Provisioning COMPLETE!"
echo " Remote JDBC URL for Student 1: jdbc:mysql://$(curl -s ifconfig.me):${DB_PORT}/${DB_NAME}"
echo " Port ${DB_PORT} is open and ready to accept remote JDBC connections."
echo "======================================================================"
