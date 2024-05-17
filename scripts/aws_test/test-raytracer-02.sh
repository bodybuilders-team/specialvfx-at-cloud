#!/bin/bash

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

function simple {
    echo "started raytracer simple"
    # Add scene.txt raw content to JSON.
	cat "$resources_dir/test01.txt" | jq -sR '{scene: .}' > payload_simple.json
    # Send the request.
	curl -s -X POST "http://$url/raytracer?scols=400\&srows=300\&wcols=400\&wrows=300\&coff=0\&roff=0\&aa=false" --data @"./payload_simple.json" > result_simple.txt
    # Remove a formatting string (remove everything before the comma).
	sed -i 's/^[^,]*,//' result_simple.txt                                                                                             
    base64 -d result_simple.txt > result_simple.bmp
    echo "finished raytracer simple"
}

function complex {
    echo "started raytrace complex"
    # Add scene.txt raw content to JSON.
	cat "$resources_dir/test-textmap.txt" | jq -sR '{scene: .}' > payload_complex.json
    # Add texmap.bmp binary to JSON (optional step, required only for some scenes).
	hexdump -ve '1/1 "%u\n"' "$resources_dir/test-texmap.bmp" | jq -s --argjson original "$(<payload_complex.json)" '$original * {texmap: .}' > payload_complex.json
    # Send the request.
	curl -s -X POST "http://$url/raytracer?scols=400\&srows=300\&wcols=400\&wrows=300\&coff=0\&roff=0\&aa=false" --data @"./payload_complex.json" > result_complex.txt
    # Remove a formatting string (remove everything before the comma).
	sed -i 's/^[^,]*,//' result_complex.txt                                                                                             
    base64 -d result_complex.txt > result_complex.bmp
    echo "finished raytracer complex"
}

# Run 100 requests concurrently and repeat 500 times
for ((i = 0; i < 50000; i++)); do
    for ((j = 0; j < 10; j++)); do
        simple &
    done
    # Wait for all background jobs to finish before starting the next batch
    wait
done
