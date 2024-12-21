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
echo "ubuntu.sxfm"
echo ""
java -jar ./lib/fm_valid_conf_gen.jar ./data/fms/ubuntu.sxfm 80 3
echo "--------------------"
echo "DONE"