#! /bin/sh

# java -mx2g -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -m Edingburgh_RTE2.all_sure.t2s.model -a s.txt -o s.json
java -mx2g -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -m Edingburgh_RTE2.all_sure.t2s.model -a s.long.txt -o s.long.json
