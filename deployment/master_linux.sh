#!/bin/bash

function new_readlink() {
    TARGET_FILE="$1"

    cd `dirname "$TARGET_FILE"`
    TARGET_FILE=`basename "$TARGET_FILE"`

    # Iterate down a (possible) chain of symlinks
    while [ -L "$TARGET_FILE" ]
    do
	TARGET_FILE=`readlink "$TARGET_FILE"`
	cd `dirname "$TARGET_FILE"`
	TARGET_FILE=`basename "$TARGET_FILE"`
    done

    # Compute the canonicalized name by finding the physical path 
    # for the directory we're in and appending the target file.
    PHYS_DIR=`pwd -P`
    RESULT=$PHYS_DIR/$TARGET_FILE
    echo $RESULT
}

JARFILE=_MASTERJAR_

SCRIPTPATH=$(new_readlink $0)

if [ -z "$SCRIPTPATH" ]; then
    MASTERDIR=$PWD
else
    MASTERDIR=$(dirname "$SCRIPTPATH")
fi

java -jar "$MASTERDIR"/$JARFILE $@

