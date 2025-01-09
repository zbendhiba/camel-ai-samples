# Camel-Python-Dumb1

## Introduction and Disclaimer
This project isn't meant to be reproduced. It's just me attempting to call  a Camel Quarkus Route from Python.

I'm not a Python developer, and I have no knowledge of best practices or  correct implementation. My goal is simply to test the basics.

This is Step 1 of my dumb tests.

If you're reading this for any reason, feedback is welcome.

I'm a Java developer, so I'll assume familiarity with Java development. If  you need help with running or compiling Java, please refer to other  resources.

## Introducing the Camel Route
This is a very simple Camel Route that returns "Hello from Camel Route!" when queried via a GET request to `http://localhost:8080/hello`.

To test it, send a GET request to this URL.

## Compile Native App
```shell
    mvn clean install -Dnative
```

## Install Python Dependencies
Please refer to the [Python Installation](/python-install) documentation  for instructions on how I installed the necessary dependencies for running Python.

## Step 1 Run the Camel Server and access the Route with URL from python code

Run the Java project locally
```shell
  cd target
  ./camel-python-dumb1-1.0.0-SNAPSHOT-runner
```

Open another terminal. Run the script.

```shell
  cd python-scripts
  python3 test-step1.py
```

You should receive the response "Hello from Camel Route!".

