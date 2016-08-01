
![zkgbai](https://cloud.githubusercontent.com/assets/3822768/11410309/23799b46-93cf-11e5-8761-e92bacbf0763.png)

zkgbai
======

This is a "Zero-K Graph-Based AI", built upon the Spring Java AI interface to play the Zero-K RTS. 

It uses a static graph derived from resource points on the game map as a major part in its decisions.

It might get a better name eventually.

Compiling
=========
The git repository contains everything necessary to build the AI except for the ant build tool. 

To compile the AI using only `ant`, simply `cd` to your checkout folder, and run `ant dist`.

Alternatively, you can import the Eclipse .project file.

Installing
==========
This AI requires version 103.0 of the Spring Engine.

To install the AI, put the files from created `/dist/` directory to `$SpringData/AI/Skirmish/ZKGBAI/dist`.

An example location on linux would be `/home/user/.spring/AI/Skirmish/ZKGBAI/dist`

**Alternatively**, you can modify the build property `installDir` to automate this, and just use `ant install` instead.

After this, any Spring lobby capable of using local AI's will be able to use this bot.
