Griswold
=========

What we did to build - 

I built the Vault and the Craftbukkit projects seperately, and then I added them to maven - using some variation of this line:

mvn install:install-file -Dfile=Vault.jar -DgroupId=net.milkbowl.vault -DartifactId=Vault -Dversion=1.16 -Dpackaging=jar

mvn install:install-file -Dfile=original-craftbukkit-1.4.7-R1.1-SNAPSHOT.jar -DgroupId=org.bukkit -DartifactId=CraftbukkitOrig -Dversion=1.4.7 -Dpackaging=jar

mvn install:install-file -Dfile=craftbukkit-1.4.7-R1.1-SNAPSHOT.jar -DgroupId=org.bukkit -DartifactId=Craftbukkit -Dversion=1.4.7 -Dpackaging=jar

From there, we added the dependencies to the maven build:
        <dependency>
            <groupId>net.milkbowl.vault</groupId>
            <artifactId>Vault</artifactId>
            <version>1.16</version>
        </dependency>
        <dependency>
            <groupId>org.bukkit</groupId>
            <artifactId>Craftbukkit</artifactId>
            <version>1.4.7</version>
        </dependency>
        <dependency>
            <groupId>org.bukkit</groupId>
            <artifactId>CraftbukkitOrig</artifactId>
            <version>1.4.7</version>
        </dependency>

You need both the Craftbukkit and the Original one.

