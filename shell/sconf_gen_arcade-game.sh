echo "SCONF Generation"
echo ""

cd ..

echo "--------------------"
echo "arcade-game"
echo ""
java -jar sconf_gen.jar -cfg ./conf/sconf_gen_arcade-game.toml
echo "--------------------"
echo "DONE arcade-game"