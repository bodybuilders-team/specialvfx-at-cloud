#!/bin/bash

source ../aws/config.sh

# CNV-23-24
# This script will issue in parallel on complex and one simple raytracer request.
# Modify it so it invokes your correct LB address and port in AWS, i.e., after http://
# If you need to change other request parameters to increase or decrease request complexity feel free to do so, provided they remain requests of different complexity.

# Check if URL argument is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <url>"
    exit 1
fi

script_dir=$(dirname "$(realpath "$0")")
resources_dir=$(realpath "$script_dir/../../app/raytracer/resources/")
url=$1

# Simple - Add scene.txt raw content to JSON.
cat "$resources_dir/test01.txt" | jq -sR '{scene: .}' > payload_simple.json

# Complex - Add texmap.bmp raw content to JSON.
cat "$resources_dir/test-texmap.txt" | jq -sR '{scene: .}' > payload_complex.json
hexdump -ve '1/1 "%u\n"' "$resources_dir/test-texmap.bmp" | jq -s --argjson original "$(<payload_complex.json)" '$original * {texmap: .}' > payload_complex.json

function simple {
  start=$(date +%s.%N)
	echo "started raytracer simple"

	# Send the request.
	curl -s -X POST "http://$url/raytracer?scols=400&srows=300&wcols=400&wrows=300&coff=0&roff=0&aa=false" --data @"./payload_simple.json" > result_raytracer_simple.txt

	# Remove a formatting string (remove everything before the comma) and decode from Base64.
	sed -i 's/^[^,]*,//' result_raytracer_simple.txt
	base64 -d result_raytracer_simple.txt > result_raytracer_simple.bmp

	echo "finished raytracer simple"

  end=$(date +%s.%N)
  duration=$(echo "$end - $start" | bc)
  echo "Execution time: $duration seconds"
}

function complex {
  start=$(date +%s.%N)
	echo "started raytrace complex"

	# Send the request.
	curl -s -X POST "http://$url/raytracer?scols=400&srows=300&wcols=400&wrows=300&coff=0&roff=0&aa=false" --data @"./payload_complex.json" > result_raytracer_complex.txt

	# Remove a formatting string (remove everything before the comma) and decode from Base64.
	sed -i 's/^[^,]*,//' result_raytracer_complex.txt
	base64 -d result_raytracer_complex.txt > result_raytracer_complex.bmp

	echo "finished raytracer complex"

  end=$(date +%s.%N)
  duration=$(echo "$end - $start" | bc)
  echo "Execution time: $duration seconds"
}

# simple
# complex

# Run multiple requests concurrently every X seconds
for ((i = 0; i < 50000; i++)); do
    for ((j = 0; j < 1; j++)); do
        simple &
    done
    sleep 3
done
