# minegrief
![mg11](https://github.com/user-attachments/assets/c5f4c746-5033-481d-926a-9ce188834042)

## Features
Encrypt Minecraft worlds and demand a Minecoin ransom

Phish connecting Minecraft players

Self-spreading to other Minecraft servers using an extendable lateral movement module system.
  - Crafty Controller Auth'd RCE - undisclosed, unpatched, intentional(?) Auth'd RCE in Crafty Controller, a panel for Minecraft server management (https://craftycontrol.com/). Check for default creds of admin:crafty or brute force.
  - SSH brute forcing - Classic SSH brute forcing, Minegrief will copy itself over SCP and execute itself if a login is successful

Persistence/stealth by "infecting" the Minecraft server jar file via manifest entry point modification (https://docs.oracle.com/javase/tutorial/deployment/jar/manifestindex.html)

Centralized command and control for system-level control of infected servers

Extendable transport system, allowing for the creation of customized C2 channels.

Works on Java 8 and above
