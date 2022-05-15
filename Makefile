run-jar:
	./gradlew assemble
	java -Dhazelcast.config=./conf/hazelcast.yaml -Dhazelcast.diagnostics.enabled=true -jar ./build/libs/yacy_grid_search-0.0.1-SNAPSHOT-all.jar
