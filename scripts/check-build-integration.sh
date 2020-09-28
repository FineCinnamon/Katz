#!/bin/bash

BRANCH=master
if [ $# -eq 1 ]; then BRANCH=$1; fi

. $BASEDIR/arrow/scripts/commons4gradle.sh
. $BASEDIR/arrow/scripts/commons4filesystem.sh

replaceOSSbyLocalRepository "$BASEDIR/arrow/gradle/*.gradle"

for repository in $(cat $BASEDIR/arrow/lists/libs.txt); do
    checkAndDownloadBranch $repository $BRANCH
    replaceGlobalPropertiesbyLocalConf $BASEDIR/$repository/gradle.properties
    runAndSaveResult $repository "Local install" "$BASEDIR/arrow/scripts/project-install.sh $repository"
done

runAndSaveResult "BOM file" "Local install" "$BASEDIR/arrow/scripts/project-install.sh arrow/BOM-file"

for repository in $(cat $BASEDIR/arrow/lists/test.txt); do
    checkAndDownloadBranch $repository $BRANCH
    replaceGlobalPropertiesbyLocalConf $BASEDIR/$repository/gradle.properties
    runAndSaveResult $repository "Check" "$BASEDIR/arrow/scripts/project-check.sh $repository"
done

showFiles
exitForResult
