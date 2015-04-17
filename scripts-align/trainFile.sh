#! /bin/sh

java -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -r train.json -d dev.json -t test.json -m /tmp/align.model
