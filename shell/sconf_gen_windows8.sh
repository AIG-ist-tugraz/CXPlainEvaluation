echo "SCONF Generation"
echo ""

cd ..

echo "--------------------"
echo "windows8"
echo ""
java -jar sconf_gen.jar -cfg ./conf/sconf_gen_windows8_1_2.toml
java -jar sconf_gen.jar -cfg ./conf/sconf_gen_windows8_4_8.toml
echo "--------------------"
echo "DONE windows8"