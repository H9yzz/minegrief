# minegrief
![mg11](https://github.com/user-attachments/assets/c5f4c746-5033-481d-926a-9ce188834042)

## Features
Encrypt Minecraft worlds and demand a Minecoin ransom.

Phish connecting Minecraft players.

Self-spreading to other Minecraft servers using an extendable, module-based lateral movement system.
  - Crafty Controller Auth'd RCE - undisclosed, unpatched, intentional(?) Auth'd RCE in Crafty Controller, a panel for Minecraft server management (https://craftycontrol.com/). Check for default creds of admin:crafty or brute force.
  - SSH brute forcing - Classic SSH brute forcing, Minegrief will copy itself over SCP and execute itself if a login is successful

Persistence/stealth by "infecting" the Minecraft server jar file via manifest entry point modification (https://docs.oracle.com/javase/tutorial/deployment/jar/manifestindex.html)

Centralized command and control for system-level control of infected servers.

Extendable transport system, allowing for the creation of customized C2 channels.

Works on Java 8 and above.


## Notes
Lateral movement modules require minegriefagent to be built as a jar file, as modules will copy itself over in a jar to spread.

I used this script to create the agent jarfile, including any requried dependencies
```bash

```

## Targeting
Minecraft servers will always have Java installed so we don't have to worry about installing it ourselves.

Infected servers will receive CIDR blocks to scan from the C2. Certain ASNs have a higher frequency of Minecraft servers (ex: Digital Ocean ASNs).  I have scraped shodan/censys to find these ASNs, the data is [here](top-asn.csv)
