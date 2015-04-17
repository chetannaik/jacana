#! /bin/sh
zcat ppdb-1.0-eng-xxxl.pruned-20.lexical.gz | awk -F " \\\|\\\|\\\| " ' { printf "%s\t%s\n", $2, $3 } ' | gzip -c > ppdb-1.0-eng-xxxl.pruned-20.lexical.pair.gz
