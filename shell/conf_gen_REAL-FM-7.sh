#
# Causality-based Explanation for Feature Model Configuration
#
# Copyright (c) 2024
#
# @author: Viet-Man Le (v.m.le@tugraz.at)
#

echo "Configuration Generation"
echo "50% preselected features"

cd ..

echo "--------------------"
echo "REAL-FM-7.splx"
echo ""
java -jar ./lib/fm_valid_conf_gen.jar ./data/fms/REAL-FM-7.splx 5 3
echo "--------------------"
echo "DONE"