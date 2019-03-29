# Factorbook Meme

A Dockerized application used by Factorbook workflows to find motifs and motif occurrences using MEME suite

## Running

The built docker container can be found on docker hub as genomealmanac/factorbook-meme.

To run make sure the files you pass are accessible and run the container with the command followed by 
the arguments you need:

`java -jar /app/meme.jar`

### Arguments

| Name |  Description |  Required | Default |
|---|---|---|---|
| `--peaks` | path to peaks in narrowPeak format | yes | |
| `--twobit` | path to two-bit file for this assembly | yes | |
| `--chrom-info` | path to chromosome lengths for this assembly | yes | |
| `--offset` | Offset, in bp, to shift peaks |  no | 0 |
| `--output-dir` | Path to write output files | yes | |

Here's how a complete command with arguments should look:

`java -jar /app/meme.jar --peaks=/data/in/my-peaks.bed --twobit=/data/in/my-twobit.2bit 
--chrom-info=/data/in/my-chrominfo.sizes --output-dir=/data/out`

## File Inputs and Outputs

See `src/test/resources` for a complete example set of input and output files

### Chrom Info Files

The required chrom info file is a tab delimited file containing chromosome names and lengths in bp. For example:

```
chr1	1000000
chr2	2000000
```

## For Contributors

The scripts/ directory contains utilities you can use to build, test, and deploy

### Building

To build the docker container with a tag, use `scripts/build-image.sh`.

### Testing

To test using and IDE, run `scripts/run-dependenies.sh` first, then run tests manually in IDE. When 
finished, run `scripts/stop-dependencies.sh`.

To test from command line, use `scripts/test.sh`. This just runs run-dependencies.sh, 
runs tests using gradle, then stop-dependencies.sh.

`scripts/run-dependenies.sh` spins up a docker container with all application dependencies like the 
Meme Suite installed. Our tests run these applications using `docker exec`.

### Deploy

To deploy the image to our docker repository, use `scripts/push-image.sh`.