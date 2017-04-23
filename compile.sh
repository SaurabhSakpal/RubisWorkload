#!/bin/bash

echo "Compile to edu/rice/rubis/client/WorkloadGenerator only selected files"
javac -cp bin/ src/edu/rice/rubis/client/RUBiSProperties.java
javac -cp bin/ src/edu/rice/rubis/client/UserSession.java
javac -cp bin/ src/edu/rice/rubis/client/WorkloadGenerator.java

