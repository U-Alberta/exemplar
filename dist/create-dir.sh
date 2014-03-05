#!/bin/bash

# Check for one argument (package name)
if [ -z "$1" ]
    then 
        echo "One argument required: package name. Ex.: exemplar-1.0"
        exit 0
fi

# Check if there is a directory for the package;
# If not, create one.
if [ ! -e "$1" ]
    then
        mkdir $1
fi

if [ -d "$1" ]
    then
        echo "Copying files to $1"
    else
        echo "Could not create the directory."
        exit 1
fi


# Copy files 
cp exemplar.jar "$1/" # jar created with create-jar.sh
cp README.txt "$1/"
cp exemplar "$1/"
cp -r sample  "$1/"

if [ ! -e "$1/data" ]
    then
        mkdir "$1/data"
fi
cp -r ../../dependencies/data/exemplar "$1/data/"
