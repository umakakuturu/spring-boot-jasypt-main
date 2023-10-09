package com.javatechie.crud.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ElasticsearchProgramSearch {

    private static final String INDEX_NAME = "programs";
    private static final String TYPE_NAME = "_doc";

    public static void main(String[] args) {
        // Simulate user input (potentially misspelled word)
        String userInput = "webms";

        // Read the CMS JSON and extract program information from the resources folder
        List<Program> programs = readCMSJson("cms.json");

        // Initialize Elasticsearch client
        try (RestHighLevelClient client = initializeClient()) {
            // Create the index if it doesn't exist
            createIndex(client);

            // Search for the program based on user input
            Program matchedProgram = searchProgram(userInput, client, programs);

            if (matchedProgram != null) {
                System.out.println("Program found: " + matchedProgram.getProgramName());
                System.out.println("Description: " + matchedProgram.getProgramDescription());
            } else {
                System.out.println("No matching program found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createIndex(RestHighLevelClient client) throws IOException {
        boolean indexExists = client.indices().exists(new GetIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);

        if (!indexExists) {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(INDEX_NAME);
            createIndexRequest.timeout(TimeValue.timeValueMinutes(5));

            client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        }
    }

    private static RestHighLevelClient initializeClient() {
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http"))
                        .setRequestConfigCallback(requestConfigBuilder ->
                                requestConfigBuilder
                                        .setConnectTimeout(5000)
                                        .setSocketTimeout(60000))
        );
    }

    private static Program searchProgram(String userInput, RestHighLevelClient client, List<Program> programs) {
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            BoolQueryBuilder caseInsensitiveQuery = QueryBuilders.boolQuery();
            BoolQueryBuilder fuzzyQuery = QueryBuilders.boolQuery();

            // Add a should clause for case-insensitive match
            caseInsensitiveQuery.should(QueryBuilders.matchQuery("keywords.keyword", userInput.toLowerCase()));

            // Add a should clause for fuzzy matching using matchQuery
            fuzzyQuery.should(QueryBuilders.matchQuery("keywords", userInput).fuzziness(Fuzziness.TWO));

            boolQueryBuilder.should(caseInsensitiveQuery);
            boolQueryBuilder.should(fuzzyQuery);

            searchSourceBuilder.query(boolQueryBuilder);

            searchRequest.source(searchSourceBuilder);

            // Debug statement to print the Elasticsearch query
            System.out.println("Elasticsearch Query: " + searchSourceBuilder.toString());

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            // Debug statement to print hits count
            System.out.println("Hits count: " + searchResponse.getHits().getHits().length);

            // Process the search response and extract the matching program
            return extractMatchingProgram(searchResponse.getHits().getHits());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Program extractMatchingProgram(org.elasticsearch.search.SearchHit[] hits) {
        // Check if hits array is not empty
        if (hits != null && hits.length > 0) {
            // Assuming that the "programName" and "programDescription" fields are stored in the source
            String programName = (String) hits[0].getSourceAsMap().get("programName");
            String programDescription = (String) hits[0].getSourceAsMap().get("programDescription");

            return new Program(programName, new ArrayList<>(), programDescription);
        }

        return null;
    }


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

    static class Program {
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
}
