package com.javatechie.crud.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class cms {

    public static void main(String[] args) {
        // Simulate user input (potentially misspelled word)
        String userInput = "blll";

        // Read the CMS JSON and extract program information from the resources folder
        List<Program> programs = readCMSJson("cms.json");

        // Search for the program based on user input
        Program matchedProgram = searchProgram(userInput, programs);

        if (matchedProgram != null) {
            System.out.println("Program found: " + matchedProgram.getProgramName());
            System.out.println("Description: " + matchedProgram.getProgramDescription());
        } else {
            System.out.println("No matching program found.");
        }
    }
  /* private static Program searchProgram(String userInput, List<Program> programs) {
        // Convert user input to lowercase for case-insensitive comparison
        String userInputLowercase = userInput.toLowerCase();

        // Search for a program that matches the user input or its variations
        for (Program program : programs) {
            // Check if the Levenshtein distance is below a certain threshold
            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
            if (program.getKeywords().stream().anyMatch(keyword ->
                    levenshteinDistance.apply(keyword.toLowerCase(), userInputLowercase) <= 4)) {
                return program;
            }
        }

        return null; // No matching program found
    }*/


    private static Program searchProgram(String userInput, List<Program> programs) {
        // Convert user input to lowercase for case-insensitive comparison
        String userInputLowercase = userInput.toLowerCase();

        Program bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        // Search for the program that has the closest matching keyword
        for (Program program : programs) {
            int minDistance = Integer.MAX_VALUE;

            // Find the minimum Levenshtein distance between the user input and each keyword
            for (String keyword : program.getKeywords()) {
                LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
                int distance = levenshteinDistance.apply(keyword.toLowerCase(), userInputLowercase);
                minDistance = Math.min(minDistance, distance);
            }

            // If the minimum distance is lower than the current best, update the best match
            if (minDistance < bestDistance) {
                bestDistance = minDistance;
                bestMatch = program;
            }

            // Check if the user input is a substring of any keyword
            if (program.getKeywords().stream().anyMatch(keyword ->
                    keyword.toLowerCase().contains(userInputLowercase) || userInputLowercase.contains(keyword.toLowerCase()))) {
                return program;
            }
        }

        // Adjust the threshold as 3 temporarily
        if (bestDistance <= 6) {
            return bestMatch;
        } else {
            return null; // No matching program found
        }
    }


   /* private static Program searchProgram(String userInput, List<Program> programs) {
        // Convert user input to lowercase for case-insensitive comparison
        String userInputLowercase = userInput.toLowerCase();

        // Search for a program that matches the user input or its variations
        for (Program program : programs) {
            // Convert program keywords to lowercase
            List<String> lowercaseKeywords = program.getKeywords().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            // Check if a significant portion of user input is contained in keywords
            long matchingKeywordsCount = lowercaseKeywords.stream()
                    .filter(userInputLowercase::contains)
                    .count();

            // Adjust the threshold as needed
            if (matchingKeywordsCount >= lowercaseKeywords.size() / 2) {
                return program;
            }
        }

        return null; // No matching program found
    }


*/

    //////////////////////////////////////////////////////////////////
    public static List<Program> readCMSJson(String jsonFileName) {
        List<Program> programs = new ArrayList<>();

        try (InputStream inputStream = cms.class.getResourceAsStream("/" + jsonFileName)) {
            if (inputStream != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode cmsJson = objectMapper.readTree(inputStream);

                // Extract program information from JSON
                for (JsonNode programNode : cmsJson.get("programs")) {
                    String programName = programNode.get("program name").asText();
                    List<String> keywords = objectMapper.convertValue(programNode.get("keywords"), ArrayList.class);
                    String programDescription = programNode.get("program description").asText();

                    programs.add(new Program(programName, keywords, programDescription));
                }
            } else {
                System.err.println("File not found: " + jsonFileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return programs;
    }
}

 class Program {
    private String programName;
    private List<String> keywords;
    private String programDescription;

    public Program(String programName, List<String> keywords, String programDescription) {
        this.programName = programName;
        this.keywords = keywords;
        this.programDescription = programDescription;
    }

    public String getProgramName() {
        return programName;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getProgramDescription() {
        return programDescription;
    }
}
