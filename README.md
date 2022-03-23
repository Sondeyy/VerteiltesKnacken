# VerteiltesKnacken
Distributed system for brute forcing an RSA-Key.

## Maintainer 
* Nils Boettcher, 5320806
* Niklas Droessler, 1646552
* Oemer Peker, 4776192

# Running it 
* Install OpenJDK 16, for example following [this instruction](https://stackoverflow.com/questions/67898586/install-java-16-on-raspberry-pi-4).
* All necessary files are in *toPi/*, go in this directory. 
* Edit the VerteiltesKnacken.conf file. It follows this structure. 
```
# yes/no, should the Client be spawned on this host?
client=no

# Integer determining how many Workers will be spawned on this Host
workerThreads=2

# Port the first Worker will be assigned to
myPort=25000

# Port all Workers try to connect to
connectionPort=25000

# IP or Address, all Workers will try to connect with
connectionAddress=localhost

# 100/1000/10000/100000
primeRange=1000
```
* start the program with `java -jar VerteiltesKnacken.jar`. Remember starting the Host with Client at last. 
