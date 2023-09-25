#!/bin/sh

ROUND_TRIP_FILES=core/target/test/ca/phon/phontalk/tests/RoundTripTests/good/
OUTPUT_FOLDER=core/target/test-project/

SCRIPT_FOLDER=$(dirname -- "$0")
CORPUS_TEMPLATE=$SCRIPT_FOLDER/__sessiontemplate.xml

ROUND_TRIP_FILES_OUTPUT=$OUTPUT_FOLDER/round-trip-files
PROJECT_OUTPUT_FOLDER=$OUTPUT_FOLDER/test-files-project.phon
CORPUS_OUTPUT_FOLDER=$PROJECT_OUTPUT_FOLDER/test-files
EXAMPLES_OUTPUT_FOLDER=$PROJECT_OUTPUT_FOLDER/examples

# Refresh output folder
if [ -d $OUTPUT_FOLDER ] ; then
	rm -rf $OUTPUT_FOLDER
fi
mkdir -p $OUTPUT_FOLDER

# Copy round trip test files to output folder
mkdir $ROUND_TRIP_FILES_OUTPUT
cp -rf $ROUND_TRIP_FILES/* $ROUND_TRIP_FILES_OUTPUT/

# create corpus folder
mkdir -p $CORPUS_OUTPUT_FOLDER
mkdir -p $EXAMPLES_OUTPUT_FOLDER

for folder in $ROUND_TRIP_FILES_OUTPUT/*; do
  testname=$(basename $folder)
  cp $folder/$testname-tb-phon.xml $CORPUS_OUTPUT_FOLDER/$testname.xml
done

cp $CORPUS_TEMPLATE $EXAMPLES_OUTPUT_FOLDER/

echo "project.name=test-files-project.phon" > $PROJECT_OUTPUT_FOLDER/project.properties
echo "project.uuid=819f8f2e-0861-4950-b5e9-84f6f13c89de" >> $PROJECT_OUTPUT_FOLDER/project.properties
