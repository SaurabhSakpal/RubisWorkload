#!/bin/bash
cd bin
# in milliseconds
java -cp . edu/rice/rubis/client/WorkloadGenerator curve_nasa_60_2 300000
cd ../
