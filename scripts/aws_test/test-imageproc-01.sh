#!/bin/bash

# CNV-23-24
# This script will issue in parallel on complex and one simple imageproc request.
# Modify it so it invokes your correct LB address and port in AWS, i.e., after http://
# If you need to change other request parameters to increase or decrease request complexity feel free to do so, provided they remain requests of different complexity.


function simple {
    echo "started imageproc simple"
    # Encode in Base64.
	base64 airplane.jpg > temp.txt                                            

	# Append a formatting string.
	echo -e "data:image/jpg;base64,$(cat temp.txt)" > temp.txt               

	# Send the request.
	curl -X POST http://127.0.0.1:8000/blurimage --data @"./temp.txt" > result.txt   

	# Remove a formatting string (remove everything before the comma).
	sed -i 's/^[^,]*,//' result.txt                                          

	# Decode from Base64.
	base64 -d result.txt > result.jpg                                 

    echo "finished imageproc simple"
}

function complex {
    echo "started imageproc complex"
    # Encode in Base64.
	base64 horse.jpg > temp.txt                                            

	# Append a formatting string.
	echo -e "data:image/jpg;base64,$(cat temp.txt)" > temp.txt               

	# Send the request.
	curl -X POST http://127.0.0.1:8000/blurimage --data @"./temp.txt" > result.txt   

	# Remove a formatting string (remove everything before the comma).
	sed -i 's/^[^,]*,//' result.txt                                          

	# Decode from Base64.
	base64 -d result.txt > result.jpg                                 

    echo "finished imageproc complex"
}

complex &
simple &
