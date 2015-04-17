# Jacana Align
###### A token-based word-aligner.

**`Source: `[`jacana`](https://code.google.com/p/jacana/wiki/JacanaAlign)**

#### Introduction
jacana-align is a token-based word-aligner for English parallel sentences described in the following paper:

[A Lightweight and High Performance Monolingual Word Aligner](http://cs.jhu.edu/~xuchen/paper/yao-jacana-wordalign-acl2013.pdf)
<br />
_\- Xuchen Yao, Benjamin Van Durme, Chris Callison-Burch and Peter Clark._
 
#### Build
jacana-align is written in a mixture of Java and Scala. If you build from ant, you have to set up the environmental variables `JAVA_HOME` and `SCALA_HOME`.

`export JAVA_HOME=/<path>/<to>/<java_7_or_higher>`
<br />
`export SCALA_HOME=/<path>/<to>/scala-2.10.2`

Then type:
<br />
`ant -f build.align.xml`
<br />
`build/lib/jacana-align.jar` will be built for you.

#### Demo

`scripts-align/runDemoServer.sh` shows up the web demo. Direct your browser to `http://localhost:8080/` and you should be able to align some sentences.

#### Align
`scripts-align/alignFile.sh` aligns tab-separated sentence files and output the output to a .json file that's accepted by the browser:

`java -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -m Edingburgh_RTE2.all_sure.t2s.model -a s.txt -o s.json`

If you need a whitespace tokenizer, use the "-n" option.

#### Training
`java -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -r train.json -d dev.json -t test.json -m /tmp/align.model`

The aligner then would train on train.json, and report F1 values on dev.json for every 10 iterations, when the stopping criterion has reached, it will test on test.json.

For every 10 iterations, a model file is saved to (in this example) /tmp/align.model.iter_XX.F1_XX.X. Select the one with the best F1 on dev.json, then run a final test on test.json:

`java -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -t test.json -m /tmp/align.model.iter_XX.F1_XX.X`

In this case since the training data is missing, the aligner assumes it's a test job, then reads model file still from the -m option, and test on test.json.
