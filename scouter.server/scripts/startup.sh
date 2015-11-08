mkdir logs
cp nohup.out > ./logs/nohup.(date '+%Y%m%d%H%M%S').out
nohup java -Xmx512m -classpath ./boot.jar scouter.boot.Boot ./lib > nohup.out &