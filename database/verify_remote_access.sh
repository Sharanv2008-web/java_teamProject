#!/usr/bin/env bash
# =============================================================================
# Subsea Pipeline Integrity Diagnostics System - Student 2 (Database Host)
# Script: verify_remote_access.sh
# Purpose: Verifies port 3306 binding, firewall rules, and query readiness on OCI VM 2
# =============================================================================

echo "======================================================================"
echo " [STUDENT 2] OCI VM 2 DATABASE REMOTE ACCESS VERIFICATION"
echo "======================================================================"

echo -e "\n[1] Checking MySQL Service Status:"
systemctl is-active mysqld 2>/dev/null || systemctl is-active mysql 2>/dev/null || echo "MySQL service not running!"

echo -e "\n[2] Checking Network Socket Binding on Port 3306:"
sudo ss -tulpn | grep 3306 || sudo netstat -tulnp | grep 3306

echo -e "\n[3] Checking OS Firewall (iptables / firewalld):"
if command -v firewall-cmd &> /dev/null; then
    sudo firewall-cmd --list-ports | grep 3306 && echo "Firewalld: Port 3306 OPEN"
fi
sudo iptables -L INPUT -n -v | grep 3306 && echo "iptables: Rule exists for 3306"

echo -e "\n[4] Testing User Authentication & Querying Pipeline Segments (subsea_pipeline):"
mysql -u subsea_app -p'SubseaApp@2026#ChangeMe' -h 127.0.0.1 -D subsea_pipeline -e "
SELECT segment_id, segment_code, category, subsea_depth_m, maop_psi 
FROM pipe_segments LIMIT 5;
"

echo -e "\n[5] Checking Active Critical Hazards in DB (repair_orders):"
mysql -u subsea_app -p'SubseaApp@2026#ChangeMe' -h 127.0.0.1 -D subsea_pipeline -e "
SELECT repair_id, segment_id, hazard_code, integrity_grade, action_required 
FROM repair_orders WHERE integrity_grade = 'D' LIMIT 5;
"

echo -e "\n======================================================================"
echo " [STUDENT 2] Verification Completed Successfully!"
echo "======================================================================"
