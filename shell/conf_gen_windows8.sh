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
echo "windows8.sxfm"
echo ""
java -jar ./lib/fm_valid_conf_gen.jar ./data/fms/windows8.sxfm 170 3
echo "--------------------"
echo "DONE"