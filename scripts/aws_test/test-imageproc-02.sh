#!/bin/bash

source ../aws/config.sh

# CNV-23-24
# This script will issue in parallel on complex and one simple imageproc request.
# Modify it so it invokes your correct LB address and port in AWS, i.e., after http://
# If you need to change other request parameters to increase or decrease request complexity feel free to do so, provided they remain requests of different complexity.

# Check if URL argument is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <url>"
    exit 1
fi

script_dir=$(dirname "$(realpath "$0")")
resources_dir=$(realpath "$script_dir/../../app/imageproc/resources/")
url=$1

# Simple - Encode image in Base64 and append a formatting string
base64 "$resources_dir/airplane.jpg" > simple_temp.txt
echo -e "data:image/jpg;base64,$(cat simple_temp.txt)" > simple_temp.txt

# Complex - Encode image in Base64 and append a formatting string
base64 "$resources_dir/horse.jpg" > complex_temp.txt
echo -e "data:image/jpg;base64,$(cat complex_temp.txt)" > complex_temp.txt

function simple {
  start=$(date +%s.%N)
  echo "started imageproc simple"

	# Send the request.
	curl -s -X POST "http://$url/enhanceimage" --data @"./simple_temp.txt" > result_imageproc_simple.txt

	# Remove a formatting string (remove everything before the comma) and decode from Base64.
	sed -i 's/^[^,]*,//' result_imageproc_simple.txt
	base64 -d result_imageproc_simple.txt > result_imageproc_simple.jpg

  echo "finished imageproc simple"

  end=$(date +%s.%N)
  duration=$(echo "$end - $start" | bc)
  echo "Execution time: $duration seconds"
}

function complex {
  start=$(date +%s.%N)
  echo "started imageproc complex"

	# Send the request.
	curl -s -X POST "http://$url/enhanceimage" --data @"./complex_temp.txt" > result_imageproc_complex.txt

	# Remove a formatting string (remove everything before the comma) and decode from Base64.
	sed -i 's/^[^,]*,//' result_imageproc_complex.txt
	base64 -d result_imageproc_complex.txt > result_imageproc_complex.jpg

  echo "finished imageproc complex"

  end=$(date +%s.%N)
  duration=$(echo "$end - $start" | bc)
  echo "Execution time: $duration seconds"
}

# simple
# complex

# Run multiple requests concurrently every X seconds
for ((i = 0; i < 500; i++)); do
    for ((j = 0; j < 4; j++)); do
        simple &
    done
    sleep 3
done