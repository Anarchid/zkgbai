zkgbai
======

This is a "Zero-K Graph-Based AI", built upon the Spring Java AI interface to play the Zero-K RTS. 

It uses a static graph derived from resource points on the game map as a major part in its decisions.

It might get a better name eventually.

Compiling
=========
The git repository contains everything necessary to build the AI except for the ant build tool. 

To compile the AI, simply `cd` to your checkout folder, and run `ant dist`.

Installing
==========
This AI will only work with Spring AI interface versions from Spring versions newer than 97.0.1-135.

You have two options to obtain such an interface:

**1)** Download and install a spring buildbot compiled binary release of Spring that is newer than 97.0.1-190 (older versions didn't build the Java interface)

**2)** Obtain and manually compile a desired Spring version, which should also build the AI interfaces.

Once this is done, put the files from created `/dist/` directory to `$SpringData/AI/Skirmish/ZKGBAI/dist`.

An example location on linux would be `/home/user/.spring/AI/Skirmish/ZKGBAI/dist`

**Alternatively**, you can modify the build property `installDir` to automate this, and just use `ant install` instead.

After this, any Spring lobby capable of using local AI's will be able to use this bot.
