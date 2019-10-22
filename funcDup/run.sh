rm *.class
javac -cp ../asm-all-5.0.3.jar funcDup.java
javac Test.java
java -cp ../asm-all-5.0.3.jar:. funcDup Test.class TestInst.class
mv TestInst.class Test.class
