# CXPlainEvaluation
Artifact for the paper "Causality-based Explanations for Feature Model Configuration", will be presented at VaMoS 2025

_Requirements_: OpenJDK 21 or higher.

## Table of Contents

- [Repository structure](#repository-structure)
- [How to reproduce the evaluation](#how-to-reproduce-the-evaluation)
    - [Execute the CXPlain evaluation by building the Docker image](#execute-the-cxplain-evaluation-by-building-the-docker-image)
    - [Generation 3 configurations of each feature model](#generation-3-configurations-of-each-feature-model)
    - [Generate SCONF](#generate-sconf)

## Repository structure

| *folder*                              | *description*                                                            |
|---------------------------------------|--------------------------------------------------------------------------|
| ./conf/                               | the configuration files for two apps: **sconf_gen** and **cxplain_eval** |
| ./data/fms                            | selected feature models                                                  |
| ./data/confs                          | generated configurations with 50% preselected features                   |
| ./data/sconfs                         | generated SCONFs                                                         |
| ./data/results/result_50.zip          | results reported in the paper                                            |
| ./lib/configurator-1.0.1-alpha-43.jar | an in-house library                                                      |
| ./lib/fm_valid_conf_gen.jar           | the program used to generate configurations                              |
| ./src                                 | source code                                                              |
| dockerfile                            | dockerfile to build the Docker image                                     |

## How to reproduce the evaluation

### Execute the CXPlain evaluation by building the Docker image

The easiest way is to build a Docker image **cxplain_eval** with the following command:

```shell
docker build -t cxplain_eval .
```
> Note 1: The Docker image does not only build the environment for the evaluation but also includes
> the evaluation execution.
>
> Note 2: The Docker building took 6 hours to complete on an Apple M1 laptop.

Next, copy evaluation results inside the Docker image to the folder `./data/results`:

```shell
docker run --rm --entrypoint tar cxplain_eval cC ./data/results . | tar xvC ./data/results
```

#### Copy sconf_gen.jar
If you want to reproduce the SCONF generation step, you should copy the **sconf_gen** program:

```shell
docker run --rm --entrypoint tar cxplain_eval cC ./target/sconf_gen.jar . | tar xvC ./sconf_gen.jar
```

#### Delete the Docker image

Delete the Docker image when you no longer need it:

```shell
docker rmi cxplain_eval
```

### Generation 3 configurations of each feature model

Shell scripts provided in `./shell` with names starting with `conf_gen` are to generate 3 configurations for each feature model in the `./data/fms` folder.
The generated configurations are stored in the `./data` folder.
You should copy them to your folder before executing another script to avoid overwriting the configurations.

### Generate SCONF

> Suppose you activated the command in [Copy sconf_gen.jar](#copy-sconf_genjar) section.

To generate SCONFs, the shell scripts with names starting with `sconf_gen_` are provided in `./shell`.
The generated SCONFs are stored in the `./data/sconfs` folder.

> Note: The SCONF generation for feature models FQA, Ubuntu, and Windows 8 will take 7-8 hours.