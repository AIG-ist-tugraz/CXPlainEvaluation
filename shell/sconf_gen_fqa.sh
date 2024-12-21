echo "SCONF Generation"
echo ""

cd ..

echo "--------------------"
echo "fqa"
echo ""
java -jar sconf_gen.jar -cfg ./conf/sconf_gen_fqa_1_2.toml
java -jar sconf_gen.jar -cfg ./conf/sconf_gen_fqa_4_8.toml
echo "--------------------"
echo "DONE fqa"