
cd `dirname $0`

mkdir out

javac src/AbstractWarsVis.java -d out

g++ --std=c++0x -W -Wall -Wno-sign-compare -pipe -mmmx -msse -msse2 -msse3 -pg src/AbstractWars.cpp
mv a.out out/

java -cp out/ AbstractWarsVis -exec out/a.out -testcase 1

gprof out/a.out gmon.out > log
