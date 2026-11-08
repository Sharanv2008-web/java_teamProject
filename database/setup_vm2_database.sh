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

DB_NAME="subsea_pipeline"
DB_USER="subsea_app"
DB_PASS="SubseaApp@2026#ChangeMe"
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
    sudo firewall-cmd --zone=public --add-port=5432/tcp --permanent || true
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
    sudo ufw allow 5432/tcp || true
fi

# 2. Configure Linux iptables rules (OCI Oracle Linux default enforces REJECT in iptables)
echo "[3/6] Ensuring iptables allows remote incoming traffic on port ${DB_PORT}..."
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport ${DB_PORT} -j ACCEPT || sudo iptables -I INPUT 1 -p tcp --dport ${DB_PORT} -j ACCEPT
sudo iptables -I INPUT 7 -m state --state NEW -p tcp --dport 5432 -j ACCEPT || true

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

# 4. Provision Databases and Remote Users for Student 1
echo "[5/6] Creating databases ('subsea_pipeline', 'subsea_db') and user privileges..."
sudo mysql -uroot <<EOF
-- Database 1: subsea_pipeline (for Student 1 App Host)
CREATE DATABASE IF NOT EXISTS subsea_pipeline CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON subsea_pipeline.* TO '${DB_USER}'@'%';
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON subsea_pipeline.* TO '${DB_USER}'@'localhost';

-- Database 2: subsea_db (for Set 30 Schema compatibility)
CREATE DATABASE IF NOT EXISTS subsea_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'subsea_user'@'%' IDENTIFIED BY 'SubseaSecurePass2026!';
GRANT ALL PRIVILEGES ON subsea_db.* TO 'subsea_user'@'%';
CREATE USER IF NOT EXISTS 'subsea_user'@'localhost' IDENTIFIED BY 'SubseaSecurePass2026!';
GRANT ALL PRIVILEGES ON subsea_db.* TO 'subsea_user'@'localhost';

FLUSH PRIVILEGES;
EOF

# 5. Ingest Schema & Seed Data
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA1="${SCRIPT_DIR}/subsea_pipeline.sql"
SCHEMA2="${SCRIPT_DIR}/subsea_pipeline_schema.sql"

if [ -f "$SCHEMA1" ]; then
    echo "[6/6] Ingesting subsea_pipeline database schema from ${SCHEMA1}..."
    sudo mysql -u"${DB_USER}" -p"${DB_PASS}" "subsea_pipeline" < "$SCHEMA1"
fi

if [ -f "$SCHEMA2" ]; then
    echo "[6/6] Ingesting subsea_db database schema from ${SCHEMA2}..."
    sudo mysql -u"subsea_user" -p"SubseaSecurePass2026!" "subsea_db" < "$SCHEMA2"
fi

echo "======================================================================"
echo " [STUDENT 2] VM 2 Database Host Provisioning COMPLETE!"
echo " Remote JDBC URL for Student 1: jdbc:mysql://$(curl -s ifconfig.me):${DB_PORT}/${DB_NAME}"
echo " Port ${DB_PORT} is open and accepting remote JDBC connections from Student 1 (VM 1)."
echo "======================================================================"
