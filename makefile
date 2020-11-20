include .env
export

run:
	sbt run

jar:
	sbt compile && sbt assembly
	cp target/scala-2.13/backend-hnv-assembly-0.1.jar backend.jar

show_env:
	@cat .env

clean:
	rm -rf *.jar target project/target project/project .metals .bloop

