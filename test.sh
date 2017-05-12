
cd `dirname $0`

mkdir out

javac src/AbstractWarsVis.java -d out

g++ --std=c++0x -W -Wall -Wno-sign-compare -O2 -s -pipe -mmmx -msse -msse2 -msse3 src/AbstractWars.cpp
mv a.out out/

java -cp out/ AbstractWarsVis -exec out/a.out
