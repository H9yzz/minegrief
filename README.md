<h1 align="center">
  <img src="https://github.com/user-attachments/assets/251374d0-b0d6-4808-91d7-9400f692101e" alt="minegrief" width="500px" height=auto>
  <br>
</h1>


## Features
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


## Notes
Lateral movement modules require minegriefagent to be built as a jar file, as modules will copy itself over in a jar to spread.

I used this script to create the agent jarfile, including any requried dependencies
```bash

```

## Targeting
Minecraft servers will always have Java installed so we don't have to worry about installing it ourselves.

Infected servers will receive CIDR blocks to scan from the C2. Certain ASNs have a higher frequency of Minecraft servers (ex: Digital Ocean ASNs).  I have scraped shodan/censys to find these ASNs, the data is [here](top-asn.csv)
