#
# Causality-based Explanation for Feature Model Configuration
#
# Copyright (c) 2024
#
# @author: Viet-Man Le (v.m.le@tugraz.at)
#

echo "SCONF Generation"
echo ""

cd ..

echo "--------------------"
echo "REAL-FM-7"
echo ""
java -jar sconf_gen.jar -cfg ./conf/sconf_gen_REAL-FM-7.toml
echo "--------------------"
echo "DONE REAL-FM-7"