#!/bin/bash

# Define the command to run
COMMAND="java -cp . -javaagent:instrumentation/target/instrumentation-1.0-SNAPSHOT-jar-with-dependencies.jar=RequestAnalyser:pt.ulisboa.tecnico.cnv.imageproc:output pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler"

OUTPUT_FILE="output.txt"

# Loop through each input image
for INPUT_IMAGE in airplane.jpg bird.jpg cat.jpg dark.png deer.jpg dog.jpg dull.jpg frog.jpg horse.jpg kodim17.jpg ship.jpg truck.jpg; do
    echo "Processing $INPUT_IMAGE..."

    # Measure the start time
    START=$(date +%s.%N)

    # Run the command with the current input image, capturing both stdout and stderr
    OUTPUT=$($COMMAND "app/imageproc/resources/$INPUT_IMAGE" "app/imageproc/resources/output_$INPUT_IMAGE" 2>&1)

    # Measure the end time
    END=$(date +%s.%N)

    # Calculate the elapsed time
    ELAPSED_TIME=$(echo "$END - $START" | bc)

    # Print the elapsed time
    echo "Time taken for $INPUT_IMAGE: $ELAPSED_TIME seconds"

    # Append the output and timing information to the output file
    echo "Input image: $INPUT_IMAGE, Time taken: $ELAPSED_TIME seconds" >> "$OUTPUT_FILE"
    echo "$OUTPUT" >> "$OUTPUT_FILE"

    echo "---------------------------------------------------"
done

echo "All images processed. Results are saved in $OUTPUT_FILE"
