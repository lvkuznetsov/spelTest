mvn clean package

copy src\main\resources\values.properties target

cd target

java -jar spelTest.jar 

>a+b
Result: 3

>SUM(a,b,"1,2,3,4")
Result: 13.0

>a+SUM("1,2,3,4")+b
Result: 13.0

>exit


