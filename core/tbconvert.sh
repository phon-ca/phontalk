#!/bin/sh
# Convert all talkbank xml files from v2 to v3 using xslt stylesheet
# Usage: tbconvert.sh <input_dir> <output_dir> 

# Check if input and output directories are provided
# If not, print usage and exit
#
if [ $# -ne 2 ]; then
    echo "Usage: $0 <input_dir> <output_dir>"
    exit 1
fi

# Check if input directory exists
# If not, print error message and exit

if [ ! -d $1 ]; then
    echo "Error: $1 is not a directory"
    exit 1
fi

# Check if output directory exists
# If not, create it
#
if [ ! -d $2 ]; then
    mkdir -p $2
fi

# path of xslt stylesheet, relative to script
XSLTFILE=./src/main/resources/tb-v3tov2.xslt

# Convert all xml files in input directory to output directory
# using xslt stylesheet.  Retain the same file name and folder structure
#
for file in `find $1 -name "*.xml"`; do
    echo "Converting $file"
    # make parent folder of file
    mkdir -p $2/`dirname $file`
    xsltproc $XSLTFILE $file > $2/$file
done

